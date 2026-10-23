package in.urlbit.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.auth.domain.User;
import in.urlbit.auth.dto.LoginRequest;
import in.urlbit.auth.repository.UserRepository;
import in.urlbit.common.error.ProblemType;
import in.urlbit.common.error.RateLimitExceededException;
import in.urlbit.links.domain.ShortUrl;
import in.urlbit.links.domain.ShortUrlStatus;
import in.urlbit.links.dto.CreateShortUrlRequest;
import in.urlbit.links.repository.ShortUrlRepository;
import in.urlbit.links.service.LinkService;
import in.urlbit.ratelimit.service.DistributedRateLimiter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class DistributedRateLimiterIT extends AbstractIntegrationTest {

    @Autowired private DistributedRateLimiter rateLimiter;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private ShortUrlRepository shortUrlRepository;
    @Autowired private LinkService linkService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = createTestUser(userRepository, "rl-user-" + UUID.randomUUID() + "@example.com");
        var httpClient =
                org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                        .disableAutomaticRetries()
                        .build();
        restTemplate
                .getRestTemplate()
                .setRequestFactory(
                        new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(
                                httpClient));
    }

    @Test
    void atomicSlidingWindowEnforcesLimitAndComputesRetryAfter() {
        String key = "test:sliding:" + UUID.randomUUID();
        int limit = 5;
        Duration window = Duration.ofSeconds(10);

        // 5 requests allowed
        for (int i = 0; i < limit; i++) {
            rateLimiter.checkLimit(key, limit, window, false);
        }

        // 6th request rejected
        assertThatThrownBy(() -> rateLimiter.checkLimit(key, limit, window, false))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(
                        ex -> {
                            RateLimitExceededException rle = (RateLimitExceededException) ex;
                            assertThat(rle.getRetryAfterSeconds()).isGreaterThan(0);
                            assertThat(rle.getRetryAfterSeconds()).isLessThanOrEqualTo(10);
                        });
    }

    @Test
    void rateLimitRejectionReturns429WithRetryAfterHeaderAndProblemDetail() {
        String ip = "198.51.100." + (int) (Math.random() * 200 + 1);

        // Direct check: exceed guest limit (30 requests)
        for (int i = 0; i < 30; i++) {
            rateLimiter.checkLimit("rl:create:ip:" + ip, 30, Duration.ofSeconds(60), true);
        }

        // Exceeded: HTTP request must return 429
        var request = new CreateShortUrlRequest("https://example.com/test", null, null);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", ip);

        var entity = new org.springframework.http.HttpEntity<>(request, headers);
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/urls", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotNull();
        assertThat(response.getBody()).contains(ProblemType.RATE_LIMITED);
    }

    @Test
    void loginRateLimiterBlocksBruteForceAfterThreshold() {
        String email = "brute-" + UUID.randomUUID() + "@example.com";
        String ip = "203.0.113." + (int) (Math.random() * 200 + 1);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", ip);

        // 10 failed login attempts allowed
        for (int i = 0; i < 10; i++) {
            var body = new LoginRequest(email, "WrongPassword" + i);
            ResponseEntity<String> res =
                    restTemplate.postForEntity(
                            "/api/v1/auth/login",
                            new org.springframework.http.HttpEntity<>(body, headers),
                            String.class);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // 11th attempt is rate-limited with 429 Too Many Requests
        var body11 = new LoginRequest(email, "WrongPassword11");
        ResponseEntity<String> res11 =
                restTemplate.postForEntity(
                        "/api/v1/auth/login",
                        new org.springframework.http.HttpEntity<>(body11, headers),
                        String.class);

        assertThat(res11.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(res11.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotNull();
        assertThat(res11.getBody()).contains(ProblemType.RATE_LIMITED);
    }

    @Test
    void multiInstanceAggregateLimitCannotBeBypassedBySwitchingInstances() {
        // Simulate two distinct application nodes (nodeA and nodeB) sharing the same Redis cluster
        DistributedRateLimiter nodeA = new DistributedRateLimiter(redisTemplate);
        DistributedRateLimiter nodeB = new DistributedRateLimiter(redisTemplate);

        String sharedKey = "rl:multi-instance:" + UUID.randomUUID();
        int aggregateLimit = 10;
        Duration window = Duration.ofSeconds(60);

        // Send 5 requests to Node A and 5 requests to Node B (Total: 10 requests)
        for (int i = 0; i < 5; i++) {
            nodeA.checkLimit(sharedKey, aggregateLimit, window, false);
            nodeB.checkLimit(sharedKey, aggregateLimit, window, false);
        }

        // 11th request on Node A is rejected
        assertThatThrownBy(() -> nodeA.checkLimit(sharedKey, aggregateLimit, window, false))
                .isInstanceOf(RateLimitExceededException.class);

        // 12th request on Node B is also rejected
        assertThatThrownBy(() -> nodeB.checkLimit(sharedKey, aggregateLimit, window, false))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void scheduledSafetyRescanAcrossInstancesProcessesDistinctLinksWithoutDuplication()
            throws Exception {
        // Pre-insert 10 active links
        List<ShortUrl> links = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ShortUrl link =
                    new ShortUrl(
                            testUser.getId(),
                            "rescan-" + UUID.randomUUID().toString().substring(0, 6),
                            "https://example.com/clean/" + i,
                            Instant.now(),
                            Instant.now(),
                            null,
                            ShortUrlStatus.ACTIVE,
                            null,
                            null);
            links.add(link);
        }
        shortUrlRepository.saveAll(links);

        // Two worker threads (representing Instance 1 and Instance 2) concurrently execute
        // rescanBatch(5)
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Callable<Integer> worker =
                () -> {
                    latch.await();
                    return linkService.rescanBatch(5);
                };

        Future<Integer> future1 = executor.submit(worker);
        Future<Integer> future2 = executor.submit(worker);

        latch.countDown();
        future1.get();
        future2.get();
        executor.shutdown();

        // Verify that all 10 links were claimed and updated with non-null last_safety_check_at
        var updated = shortUrlRepository.findAllById(links.stream().map(ShortUrl::getId).toList());
        assertThat(updated).hasSize(10);
        assertThat(updated).allMatch(l -> l.getLastSafetyCheckAt() != null);
    }
}
