package in.urlbit.auth;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.auth.domain.User;
import in.urlbit.auth.domain.UserRole;
import in.urlbit.auth.domain.UserStatus;
import in.urlbit.auth.repository.UserRepository;
import in.urlbit.links.dto.CreateShortUrlRequest;
import in.urlbit.links.dto.ShortUrlResponse;
import in.urlbit.security.JwtTokenProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Verifies the single-column role parity requirement (Spec §10).
 *
 * <p>All authenticated roles (USER, TENANT, ADMIN) have identical capabilities, and ADMIN has no
 * bypass around link ownership (IDOR checks apply equally to all roles).
 */
class RoleParityIT extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private UserRepository userRepository;

    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Test
    void adminAndUserHaveIdenticalCapabilitiesAndAdminCannotAccessUserLink() {
        // Create a regular USER
        User regularUser =
                userRepository.save(
                        new User(
                                UUID.randomUUID(),
                                "regular-" + UUID.randomUUID() + "@example.com",
                                "hash",
                                Instant.now(),
                                Instant.now(),
                                UserStatus.ACTIVE,
                                UserRole.USER));
        String userToken =
                jwtTokenProvider.generateAccessToken(
                        regularUser.getId(), regularUser.getEmail(), regularUser.getRole());

        // Create an ADMIN user
        User adminUser =
                userRepository.save(
                        new User(
                                UUID.randomUUID(),
                                "admin-" + UUID.randomUUID() + "@example.com",
                                "hash",
                                Instant.now(),
                                Instant.now(),
                                UserStatus.ACTIVE,
                                UserRole.ADMIN));
        String adminToken =
                jwtTokenProvider.generateAccessToken(
                        adminUser.getId(), adminUser.getEmail(), adminUser.getRole());

        // Regular user creates a link with custom alias
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        var createReq =
                new CreateShortUrlRequest(
                        "https://example.com/regular-page",
                        "custom-" + UUID.randomUUID().toString().substring(0, 6),
                        null);

        ResponseEntity<ShortUrlResponse> userCreateRes =
                restTemplate.exchange(
                        "/api/v1/urls",
                        HttpMethod.POST,
                        new HttpEntity<>(createReq, userHeaders),
                        ShortUrlResponse.class);

        assertThat(userCreateRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long userLinkId = userCreateRes.getBody().id();

        // ADMIN creates a link with custom alias (identical capability)
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        var adminCreateReq =
                new CreateShortUrlRequest(
                        "https://example.com/admin-page",
                        "admin-" + UUID.randomUUID().toString().substring(0, 6),
                        null);

        ResponseEntity<ShortUrlResponse> adminCreateRes =
                restTemplate.exchange(
                        "/api/v1/urls",
                        HttpMethod.POST,
                        new HttpEntity<>(adminCreateReq, adminHeaders),
                        ShortUrlResponse.class);

        assertThat(adminCreateRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Role Parity Check: ADMIN cannot access USER's link (strict ownership, no role bypass)
        ResponseEntity<String> adminAccessRes =
                restTemplate.exchange(
                        "/api/v1/urls/" + userLinkId,
                        HttpMethod.GET,
                        new HttpEntity<>(adminHeaders),
                        String.class);

        assertThat(adminAccessRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
