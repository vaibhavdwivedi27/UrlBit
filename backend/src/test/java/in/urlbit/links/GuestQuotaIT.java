package in.urlbit.links;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.links.dto.CreateShortUrlRequest;
import in.urlbit.links.dto.ShortUrlResponse;
import in.urlbit.links.service.GuestCookieUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GuestQuotaIT extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void guestCanCreateFiveLinksAndSixthIsRejectedWith429() {
        String guestId = "test-guest-" + java.util.UUID.randomUUID();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, GuestCookieUtil.COOKIE_NAME + "=" + guestId);

        // First 5 creations must succeed with 201 Created
        for (int i = 1; i <= 5; i++) {
            var body = new CreateShortUrlRequest("https://example.com/page/" + i, null, null);
            var entity = new HttpEntity<>(body, headers);

            ResponseEntity<ShortUrlResponse> response =
                    restTemplate.exchange(
                            "/api/v1/urls", HttpMethod.POST, entity, ShortUrlResponse.class);

            assertThat(response.getStatusCode())
                    .withFailMessage(
                            "Creation " + i + " failed with status " + response.getStatusCode())
                    .isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).hasSize(7);
        }

        // 6th creation must fail with 429 Too Many Requests
        var sixthBody = new CreateShortUrlRequest("https://example.com/page/6", null, null);
        var sixthEntity = new HttpEntity<>(sixthBody, headers);

        ResponseEntity<String> sixthResponse =
                restTemplate.exchange("/api/v1/urls", HttpMethod.POST, sixthEntity, String.class);

        assertThat(sixthResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(sixthResponse.getBody()).contains("Guest creation quota");
    }

    @Test
    void guestSuppliedAliasIsRejected() {
        String guestId = "test-guest-" + java.util.UUID.randomUUID();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, GuestCookieUtil.COOKIE_NAME + "=" + guestId);

        var body =
                new CreateShortUrlRequest(
                        "https://example.com/alias-test", "my-custom-alias", null);
        var entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.exchange("/api/v1/urls", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Custom aliases require an account");
    }
}
