package in.urlbit.links.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.urlbit.common.error.UnsafeDestinationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DestinationSafetyValidatorTest {

    private DestinationSafetyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DestinationSafetyValidator();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://example.com/articles/java",
                "http://github.com/spring-projects",
                "https://sub.domain.org/path?q=test#anchor"
            })
    void acceptsValidHttpAndHttpsUrls(String url) {
        assertThatCode(() -> validator.validate(url)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ftp://files.example.com/archive.zip",
                "javascript:void(0)",
                "gopher://example.com/item",
                "file:///etc/passwd"
            })
    void rejectsNonHttpSchemes(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(UnsafeDestinationException.class)
                .hasMessageContaining("Only http and https URL schemes are supported");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://urlbit.in/destination",
                "https://sub.urlbit.in/link",
                "http://URLBIT.IN/test",
                "https://nested.sub.urlbit.in."
            })
    void rejectsSelfDomainAndSubdomains(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(UnsafeDestinationException.class)
                .hasMessageContaining("Links to urlbit.in and its subdomains are not allowed");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"http://127.0.0.1/admin", "http://localhost:8080", "http://0.0.0.0/debug"})
    void rejectsLoopbackAndPrivateIpLiterals(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(UnsafeDestinationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://bit.ly/3xyz", "https://tinyurl.com/abc"})
    void rejectsDenylistedHosts(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(UnsafeDestinationException.class)
                .hasMessageContaining("blocklist");
    }

    @Test
    void rejectsUserCredentialsInUrl() {
        assertThatThrownBy(() -> validator.validate("https://admin:secret@example.com/dashboard"))
                .isInstanceOf(UnsafeDestinationException.class)
                .hasMessageContaining("credentials");
    }
}
