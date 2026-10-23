package in.urlbit.links.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.urlbit.config.CacheProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Cache-aside operations on Redis for redirect resolution.
 *
 * <p>Key format: {@code link:{code}}. Values are JSON-serialized {@link CachedLink} records. TTL is
 * bounded by configured TTL and the link's explicit expiration timestamp. Any Redis outage falls
 * back gracefully to PostgreSQL (fail-open) and records an error metric.
 */
@Service
public class LinkCacheService {

    private static final Logger log = LoggerFactory.getLogger(LinkCacheService.class);
    private static final String KEY_PREFIX = "link:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;

    private final Counter hitsCounter;
    private final Counter missesCounter;
    private final Counter errorsCounter;

    public LinkCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CacheProperties cacheProperties,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;

        this.hitsCounter =
                Counter.builder("cache.hits").tag("cache", "links").register(meterRegistry);
        this.missesCounter =
                Counter.builder("cache.misses").tag("cache", "links").register(meterRegistry);
        this.errorsCounter =
                Counter.builder("cache.errors").tag("cache", "links").register(meterRegistry);
    }

    public Optional<CachedLink> get(String code) {
        String key = KEY_PREFIX + code;
        try {
            String rawJson = redisTemplate.opsForValue().get(key);
            if (rawJson == null) {
                missesCounter.increment();
                return Optional.empty();
            }

            CachedLink cached = objectMapper.readValue(rawJson, CachedLink.class);
            hitsCounter.increment();
            return Optional.of(cached);
        } catch (Exception ex) {
            errorsCounter.increment();
            log.warn(
                    "Redis read failed for key '{}', falling back to database: {}",
                    key,
                    ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(String code, CachedLink link) {
        String key = KEY_PREFIX + code;
        try {
            Duration ttl;
            if (!link.isPresent()) {
                ttl = cacheProperties.getNegativeTtl();
            } else {
                Instant now = Instant.now();
                if (link.expiresAt() != null) {
                    if (!link.expiresAt().isAfter(now)) {
                        // Already expired link; do not cache
                        return;
                    }
                    long secondsUntilExpiry = Duration.between(now, link.expiresAt()).toSeconds();
                    long configuredSeconds = cacheProperties.getLinkTtl().toSeconds();
                    long ttlSeconds = Math.min(configuredSeconds, secondsUntilExpiry);
                    if (ttlSeconds <= 0) {
                        return;
                    }
                    ttl = Duration.ofSeconds(ttlSeconds);
                } else {
                    ttl = cacheProperties.getLinkTtl();
                }
            }

            String json = objectMapper.writeValueAsString(link);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize CachedLink for code '{}'", code, ex);
        } catch (Exception ex) {
            errorsCounter.increment();
            log.warn("Redis write failed for key '{}': {}", key, ex.getMessage());
        }
    }

    public void evict(String code) {
        String key = KEY_PREFIX + code;
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            errorsCounter.increment();
            log.warn("Redis eviction failed for key '{}': {}", key, ex.getMessage());
        }
    }
}
