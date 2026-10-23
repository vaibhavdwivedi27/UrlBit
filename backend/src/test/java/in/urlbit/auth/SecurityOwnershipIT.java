package in.urlbit.auth;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.auth.dto.AuthResponse;
import in.urlbit.auth.dto.LoginRequest;
import in.urlbit.auth.dto.SignupRequest;
import in.urlbit.links.domain.ShortUrlStatus;
import in.urlbit.links.dto.CreateShortUrlRequest;
import in.urlbit.links.dto.ShortUrlResponse;
import in.urlbit.links.dto.UpdateShortUrlRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SecurityOwnershipIT extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void userCannotAccessOrModifyAnotherUsersLinks() {
        // Register and login User A
        String emailA = "usera-" + UUID.randomUUID() + "@example.com";
        String tokenA = registerAndGetToken(emailA);

        // Register and login User B
        String emailB = "userb-" + UUID.randomUUID() + "@example.com";
        String tokenB = registerAndGetToken(emailB);

        // User A creates a link
        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);
        var createReq =
                new CreateShortUrlRequest(
                        "https://example.com/target-a",
                        "link-a-" + UUID.randomUUID().toString().substring(0, 6),
                        null);
        ResponseEntity<ShortUrlResponse> createRes =
                restTemplate.exchange(
                        "/api/v1/urls",
                        HttpMethod.POST,
                        new HttpEntity<>(createReq, headersA),
                        ShortUrlResponse.class);

        assertThat(createRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long linkIdA = createRes.getBody().id();

        // User B attempts IDOR: GET User A's link by ID -> Must return 404 (existence not
        // disclosed)
        HttpHeaders headersB = new HttpHeaders();
        headersB.setBearerAuth(tokenB);
        ResponseEntity<String> idorGetRes =
                restTemplate.exchange(
                        "/api/v1/urls/" + linkIdA,
                        HttpMethod.GET,
                        new HttpEntity<>(headersB),
                        String.class);
        assertThat(idorGetRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // User B attempts to UPDATE User A's link -> Must return 404
        var updateReq = new UpdateShortUrlRequest(ShortUrlStatus.DISABLED, null);
        ResponseEntity<String> idorUpdateRes =
                restTemplate.exchange(
                        "/api/v1/urls/" + linkIdA,
                        HttpMethod.PATCH,
                        new HttpEntity<>(updateReq, headersB),
                        String.class);
        assertThat(idorUpdateRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // User B attempts to DELETE User A's link -> Must return 404
        ResponseEntity<String> idorDeleteRes =
                restTemplate.exchange(
                        "/api/v1/urls/" + linkIdA,
                        HttpMethod.DELETE,
                        new HttpEntity<>(headersB),
                        String.class);
        assertThat(idorDeleteRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // User A successfully retrieves their own link
        ResponseEntity<ShortUrlResponse> ownerGetRes =
                restTemplate.exchange(
                        "/api/v1/urls/" + linkIdA,
                        HttpMethod.GET,
                        new HttpEntity<>(headersA),
                        ShortUrlResponse.class);
        assertThat(ownerGetRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String registerAndGetToken(String email) {
        String password = "Password123!";
        restTemplate.postForEntity(
                "/api/v1/auth/signup", new SignupRequest(email, password), String.class);
        ResponseEntity<AuthResponse> loginRes =
                restTemplate.postForEntity(
                        "/api/v1/auth/login",
                        new LoginRequest(email, password),
                        AuthResponse.class);
        return loginRes.getBody().accessToken();
    }
}
