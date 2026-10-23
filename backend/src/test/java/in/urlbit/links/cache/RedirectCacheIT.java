package in.urlbit.links.cache;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.auth.domain.User;
import in.urlbit.auth.repository.UserRepository;
import in.urlbit.links.domain.ShortUrl;
import in.urlbit.links.domain.ShortUrlStatus;
import in.urlbit.links.dto.CreateShortUrlRequest;
import in.urlbit.links.dto.UpdateShortUrlRequest;
import in.urlbit.links.repository.ShortUrlRepository;
import in.urlbit.links.service.LinkService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedirectCacheIT extends AbstractIntegrationTest {

    @Autowired private LinkService linkService;
    @Autowired private ShortUrlRepository shortUrlRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StringRedisTemplate redisTemplate;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser =
                createTestUser(userRepository, "cache-test-" + UUID.randomUUID() + "@example.com");
    }

    @Test
    void cacheMissPopulatesRedisAndSubsequentLookupHitsCache() {
        String code = "hit-" + UUID.randomUUID().toString().substring(0, 6);
        String destination = "https://example.com/cached-target";

        ShortUrl link =
                new ShortUrl(
                        testUser.getId(),
                        code,
                        destination,
                        Instant.now(),
                        Instant.now(),
                        null,
                        ShortUrlStatus.ACTIVE,
                        null,
                        Instant.now());
        shortUrlRepository.save(link);

        // Before lookup, key does not exist in Redis
        assertThat(redisTemplate.hasKey("link:" + code)).isFalse();

        // 1st lookup: cache miss -> queries PostgreSQL -> populates Redis
        Optional<String> firstResult = linkService.resolveRedirect(code);
        assertThat(firstResult).contains(destination);
        assertThat(redisTemplate.hasKey("link:" + code)).isTrue();

        // 2nd lookup: cache hit directly from Redis
        Optional<String> secondResult = linkService.resolveRedirect(code);
        assertThat(secondResult).contains(destination);
    }

    @Test
    void cacheTtlNeverOutlivesLinkExpiration() {
        String code = "ttl-" + UUID.randomUUID().toString().substring(0, 6);
        String destination = "https://example.com/expiring-target";
        Instant expiresAt = Instant.now().plus(300, ChronoUnit.SECONDS); // 5 minutes

        ShortUrl link =
                new ShortUrl(
                        testUser.getId(),
                        code,
                        destination,
                        Instant.now(),
                        Instant.now(),
                        expiresAt,
                        ShortUrlStatus.ACTIVE,
                        null,
                        Instant.now());
        shortUrlRepository.save(link);

        linkService.resolveRedirect(code);

        Long ttl = redisTemplate.getExpire("link:" + code, TimeUnit.SECONDS);
        assertThat(ttl).isNotNull();
        // TTL must be bounded by link expiry (300 seconds) and not exceed it
        assertThat(ttl).isLessThanOrEqualTo(300);
        assertThat(ttl).isGreaterThan(0);
    }

    @Test
    void negativeCacheStoresMissAndIsEvictedWhenAliasIsCreated() {
        String code = "neg-" + UUID.randomUUID().toString().substring(0, 6);

        // 1. Query unknown code -> empty result
        Optional<String> miss = linkService.resolveRedirect(code);
        assertThat(miss).isEmpty();

        // Negative entry is cached in Redis with short TTL
        assertThat(redisTemplate.hasKey("link:" + code)).isTrue();

        // 2. Create link with that exact custom alias
        var createReq = new CreateShortUrlRequest("https://example.com/now-exists", code, null);
        linkService.create(createReq, testUser.getId(), null, "127.0.0.1");

        // Negative cache was evicted post-commit
        assertThat(redisTemplate.hasKey("link:" + code)).isFalse();

        // 3. Query again -> now resolves to newly created URL
        Optional<String> hit = linkService.resolveRedirect(code);
        assertThat(hit).contains("https://example.com/now-exists");
    }

    @Test
    void postCommitInvalidationEvictsCacheOnUpdateAndTakedown() {
        String code = "evict-" + UUID.randomUUID().toString().substring(0, 6);
        String destination = "https://example.com/target-to-evict";

        ShortUrl link =
                new ShortUrl(
                        testUser.getId(),
                        code,
                        destination,
                        Instant.now(),
                        Instant.now(),
                        null,
                        ShortUrlStatus.ACTIVE,
                        null,
                        Instant.now());
        ShortUrl saved = shortUrlRepository.save(link);

        // Populate cache
        linkService.resolveRedirect(code);
        assertThat(redisTemplate.hasKey("link:" + code)).isTrue();

        // Update status to DISABLED -> post-commit evicts cache
        linkService.update(
                saved.getId(),
                testUser.getId(),
                new UpdateShortUrlRequest(ShortUrlStatus.DISABLED, null));
        assertThat(redisTemplate.hasKey("link:" + code)).isFalse();

        // Subsequent lookup returns empty
        assertThat(linkService.resolveRedirect(code)).isEmpty();

        // Operator takedown also evicts cache
        link.setStatus(ShortUrlStatus.ACTIVE);
        shortUrlRepository.save(link);
        linkService.resolveRedirect(code);
        assertThat(redisTemplate.hasKey("link:" + code)).isTrue();

        linkService.takedown(code, "Malware takedown");
        assertThat(redisTemplate.hasKey("link:" + code)).isFalse();
        assertThat(linkService.resolveRedirect(code)).isEmpty();
    }

    @Test
    void redisOutageFailsOpenToPostgresWithoutFailingRedirect() {
        String code = "outage-" + UUID.randomUUID().toString().substring(0, 6);
        String destination = "https://example.com/outage-survivor";

        ShortUrl link =
                new ShortUrl(
                        testUser.getId(),
                        code,
                        destination,
                        Instant.now(),
                        Instant.now(),
                        null,
                        ShortUrlStatus.ACTIVE,
                        null,
                        Instant.now());
        shortUrlRepository.save(link);

        // Pause Redis container to simulate complete network/server outage
        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            // Must not throw an exception; must gracefully fall back to PostgreSQL
            Optional<String> result = linkService.resolveRedirect(code);
            assertThat(result).contains(destination);
        } finally {
            // Unpause Redis container to restore test environment
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
    }
}
