package in.urlbit.links.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReservedPathValidatorTest {

    private ReservedPathValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ReservedPathValidator();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "api",
                "login",
                "signup",
                "dashboard",
                "settings",
                "health",
                "actuator",
                "swagger-ui"
            })
    void rejectsExactReservedWords(String path) {
        assertThat(validator.isReserved(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOGIN", "Dashboard", "API", "Health", "SwAgGeR-UI"})
    void rejectsCaseInsensitiveReservedWords(String path) {
        assertThat(validator.isReserved(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"my-link", "article2026", "project-x", "awesome_url", "gh-12345"})
    void allowsNonReservedWords(String path) {
        assertThat(validator.isReserved(path)).isFalse();
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(validator.isReserved(null)).isFalse();
        assertThat(validator.isReserved("   ")).isFalse();
    }
}
