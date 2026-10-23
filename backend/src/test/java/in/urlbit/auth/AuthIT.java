package in.urlbit.auth;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.auth.domain.UserRole;
import in.urlbit.auth.dto.AuthResponse;
import in.urlbit.auth.dto.LoginRequest;
import in.urlbit.auth.dto.RefreshTokenRequest;
import in.urlbit.auth.dto.SignupRequest;
import in.urlbit.auth.dto.UserResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthIT extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void signupLoginRefreshAndReplayDetectionFlow() {
        String email = "flow-" + UUID.randomUUID() + "@example.com";
        String password = "SecurePassword123!";

        // 1. Signup
        var signupReq = new SignupRequest(email, password);
        ResponseEntity<UserResponse> signupRes =
                restTemplate.postForEntity("/api/v1/auth/signup", signupReq, UserResponse.class);

        assertThat(signupRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(signupRes.getBody()).isNotNull();
        assertThat(signupRes.getBody().email()).isEqualTo(email.toLowerCase());
        assertThat(signupRes.getBody().role()).isEqualTo(UserRole.USER);

        // Duplicate signup rejected with 409
        ResponseEntity<String> duplicateRes =
                restTemplate.postForEntity("/api/v1/auth/signup", signupReq, String.class);
        assertThat(duplicateRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 2. Login
        var loginReq = new LoginRequest(email, password);
        ResponseEntity<AuthResponse> loginRes =
                restTemplate.postForEntity("/api/v1/auth/login", loginReq, AuthResponse.class);

        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginRes.getBody()).isNotNull();
        String initialAccess = loginRes.getBody().accessToken();
        String initialRefresh = loginRes.getBody().refreshToken();
        assertThat(initialAccess).isNotBlank();
        assertThat(initialRefresh).isNotBlank();

        // 3. /me endpoint with Bearer token
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(initialAccess);
        ResponseEntity<UserResponse> meRes =
                restTemplate.exchange(
                        "/api/v1/me",
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders),
                        UserResponse.class);
        assertThat(meRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meRes.getBody()).isNotNull();
        assertThat(meRes.getBody().email()).isEqualTo(email.toLowerCase());

        // 4. Token Refresh (Rotation)
        var refreshReq = new RefreshTokenRequest(initialRefresh);
        ResponseEntity<AuthResponse> refreshRes =
                restTemplate.postForEntity("/api/v1/auth/refresh", refreshReq, AuthResponse.class);

        assertThat(refreshRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshRes.getBody()).isNotNull();
        String rotatedRefresh = refreshRes.getBody().refreshToken();
        assertThat(rotatedRefresh).isNotEqualTo(initialRefresh);

        // 5. Replay Attack Detection: Reusing old refresh token must trigger family revocation!
        ResponseEntity<String> replayRes =
                restTemplate.postForEntity("/api/v1/auth/refresh", refreshReq, String.class);
        assertThat(replayRes.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Subsequent refresh with the newer token must ALSO fail because the whole family was
        // revoked
        var rotatedRefreshReq = new RefreshTokenRequest(rotatedRefresh);
        ResponseEntity<String> revokedFamilyRes =
                restTemplate.postForEntity("/api/v1/auth/refresh", rotatedRefreshReq, String.class);
        assertThat(revokedFamilyRes.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        var loginReq = new LoginRequest("nobody@example.com", "WrongPassword!");
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/auth/login", loginReq, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
