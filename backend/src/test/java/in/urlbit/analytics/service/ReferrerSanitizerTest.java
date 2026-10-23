package in.urlbit.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ReferrerSanitizerTest {

    private ReferrerSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new ReferrerSanitizer();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void blankReferrerReturnsDirect(String input) {
        assertThat(sanitizer.sanitize(input)).isEqualTo("direct");
    }

    @ParameterizedTest
    @CsvSource({
        "https://www.google.com/search?q=test&safe=active, www.google.com",
        "https://sub.DOMAIN.com:8443/deep/path#section, sub.domain.com",
        "http://github.com/org/repo, github.com",
        "https://news.ycombinator.com/, news.ycombinator.com",
        "https://trailing-dot.example.org., trailing-dot.example.org"
    })
    void extractsAndNormalizesHost(String input, String expected) {
        assertThat(sanitizer.sanitize(input)).isEqualTo(expected);
    }

    @Test
    void handlesSchemelessDomain() {
        assertThat(sanitizer.sanitize("twitter.com/i/web/status/123")).isEqualTo("twitter.com");
    }

    @Test
    void normalizesInternationalizedDomainNames() {
        // IDN München -> xn--mnchen-3ya.de
        assertThat(sanitizer.sanitize("https://münchen.de/events")).isEqualTo("xn--mnchen-3ya.de");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "about:blank",
                "javascript:void(0)",
                "data:text/html,test",
                "invalid url with spaces %%",
                "://missing-scheme",
                "file:///etc/hosts"
            })
    void invalidOrOpaqueReferrerFallsBackToDirect(String input) {
        assertThat(sanitizer.sanitize(input)).isEqualTo("direct");
    }
}
