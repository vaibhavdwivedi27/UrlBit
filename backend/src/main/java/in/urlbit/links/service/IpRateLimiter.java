package in.urlbit.links.service;

import in.urlbit.common.error.RateLimitExceededException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory rate limiter for anonymous creation traffic.
 *
 * <p>Keys IPv4 traffic by full IP address and IPv6 traffic by /64 prefix to prevent subscriber pool
 * rotation bypasses. Keys strictly on socket remote address.
 */
@Component
public class IpRateLimiter {

    private static final int MAX_REQUESTS_PER_WINDOW = 30;
    private static final long WINDOW_SECONDS = 60;

    private final ConcurrentHashMap<String, WindowCounter> buckets = new ConcurrentHashMap<>();

    public void checkLimit(String remoteIp) {
        String key = deriveRateLimitKey(remoteIp);
        long nowSeconds = Instant.now().getEpochSecond();

        WindowCounter counter =
                buckets.compute(
                        key,
                        (k, existing) -> {
                            if (existing == null
                                    || nowSeconds - existing.windowStartSeconds >= WINDOW_SECONDS) {
                                return new WindowCounter(nowSeconds, 1);
                            }
                            existing.count++;
                            return existing;
                        });

        if (counter.count > MAX_REQUESTS_PER_WINDOW) {
            long retryAfter = WINDOW_SECONDS - (nowSeconds - counter.windowStartSeconds);
            throw new RateLimitExceededException(
                    "Creation rate limit exceeded. Please wait before creating more links.",
                    Math.max(1, retryAfter));
        }
    }

    public String deriveRateLimitKey(String ip) {
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

    private static class WindowCounter {
        long windowStartSeconds;
        int count;

        WindowCounter(long windowStartSeconds, int count) {
            this.windowStartSeconds = windowStartSeconds;
            this.count = count;
        }
    }
}
