package in.urlbit;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests. Starts a singleton PostgreSQL Testcontainer shared across all
 * test classes to avoid per-class container overhead. Subclasses inherit the Spring Boot test
 * context with a random port and the "test" profile.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("urlbit_test")
                    .withUsername("urlbit_test")
                    .withPassword("urlbit_test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    public in.urlbit.auth.domain.User createTestUser(
            in.urlbit.auth.repository.UserRepository userRepository, String email) {
        var user =
                new in.urlbit.auth.domain.User(
                        java.util.UUID.randomUUID(),
                        email,
                        "test_hash",
                        java.time.Instant.now(),
                        java.time.Instant.now(),
                        in.urlbit.auth.domain.UserStatus.ACTIVE,
                        in.urlbit.auth.domain.UserRole.USER);
        return userRepository.save(user);
    }
}
