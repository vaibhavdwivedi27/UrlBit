package in.urlbit.ratelimit.service;

import in.urlbit.common.error.RateLimitExceededException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Distributed rate limiter using Redis and an atomic Lua sliding-window script.
 *
 * <p>Supports per-user and per-IP throttling across multiple application instances without race
 * conditions. Groups IPv6 addresses under /64 subnets to prevent residential pool rotation attacks.
 */
@Service
public class DistributedRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(DistributedRateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> rateLimiterScript;

    public DistributedRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimiterScript = new DefaultRedisScript<>();
        this.rateLimiterScript.setLocation(new ClassPathResource("scripts/rate_limiter.lua"));
        this.rateLimiterScript.setResultType(List.class);
    }

    public void checkLimit(String key, int limit, Duration window, boolean failOpen) {
        long nowMs = Instant.now().toEpochMilli();
        long windowMs = window.toMillis();
        String member = UUID.randomUUID().toString();

        try {
            List<?> result =
                    redisTemplate.execute(
                            rateLimiterScript,
                            Collections.singletonList(key),
                            String.valueOf(nowMs),
                            String.valueOf(windowMs),
                            String.valueOf(limit),
                            member);

            if (result == null || result.isEmpty()) {
                return;
            }

            long allowed = ((Number) result.get(0)).longValue();
            if (allowed == 0) {
                long retryAfter = ((Number) result.get(2)).longValue();
                log.warn(
                        "Rate limit exceeded for key '{}' (limit: {}, retryAfter: {}s)",
                        key,
                        limit,
                        retryAfter);
                throw new RateLimitExceededException(
                        "Rate limit exceeded. Please wait before retrying.", retryAfter);
            }
        } catch (RateLimitExceededException ex) {
            throw ex;
        } catch (Exception ex) {
            if (failOpen) {
                log.warn(
                        "Redis rate-limiter unavailable, failing open for key '{}': {}",
                        key,
                        ex.getMessage());
            } else {
                log.error(
                        "Redis rate-limiter unavailable, failing closed for key '{}': {}",
                        key,
                        ex.getMessage());
                throw new RateLimitExceededException(
                        "Service temporarily unavailable due to rate-limiting failure", 30);
            }
        }
    }

    public String deriveIpKey(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        String cleanIp = ip.strip();
        try {
            InetAddress address = InetAddress.getByName(cleanIp);
            if (address instanceof Inet6Address) {
                byte[] bytes = address.getAddress();
                // Mask lower 64 bits (bytes 8 through 15) to group the /64 prefix
                byte[] masked = Arrays.copyOf(bytes, bytes.length);
                Arrays.fill(masked, 8, 16, (byte) 0);
                return InetAddress.getByAddress(masked).getHostAddress() + "/64";
            }
            return address.getHostAddress();
        } catch (UnknownHostException ex) {
            return cleanIp;
        }
    }
}
