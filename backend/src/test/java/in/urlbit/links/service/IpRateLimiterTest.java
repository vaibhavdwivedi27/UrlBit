package in.urlbit.links.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.urlbit.common.error.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IpRateLimiterTest {

    private IpRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new IpRateLimiter();
    }

    @Test
    void allowsRequestsUpToThreshold() {
        String ip = "192.0.2.1";
        for (int i = 0; i < 30; i++) {
            assertThatCode(() -> limiter.checkLimit(ip)).doesNotThrowAnyException();
        }
    }

    @Test
    void blocksRequestExceedingThreshold() {
        String ip = "192.0.2.2";
        for (int i = 0; i < 30; i++) {
            limiter.checkLimit(ip);
        }
        assertThatThrownBy(() -> limiter.checkLimit(ip))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("rate limit exceeded");
    }

    @Test
    void groupsIpv6AddressesUnderSameSlash64Subnet() {
        // Two addresses in the same /64 subnet
        String ip1 = "2001:db8:abcd:0012:0000:0000:0000:0001";
        String ip2 = "2001:db8:abcd:0012:ffff:ffff:ffff:ffff";

        String key1 = limiter.deriveRateLimitKey(ip1);
        String key2 = limiter.deriveRateLimitKey(ip2);

        assertThat(key1).isEqualTo(key2);
    }
}
