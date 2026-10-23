package in.urlbit.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.analytics.dto.ShortUrlAnalyticsResponse;
import in.urlbit.analytics.service.AsyncClickEventBuffer;
import in.urlbit.analytics.service.ClickBatchFlusher;
import in.urlbit.auth.dto.AuthResponse;
import in.urlbit.auth.dto.LoginRequest;
import in.urlbit.auth.dto.SignupRequest;
import in.urlbit.links.dto.CreateShortUrlRequest;
import in.urlbit.links.dto.ShortUrlResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

class ClickAnalyticsIT extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ClickBatchFlusher clickBatchFlusher;
    @Autowired private AsyncClickEventBuffer clickEventBuffer;

    @BeforeEach
    void setUp() {
        var httpClient =
                HttpClients.custom()
                        .disableRedirectHandling()
                        .disableAutomaticRetries()
                        .disableCookieManagement()
                        .build();
        restTemplate
                .getRestTemplate()
                .setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Test
    void redirectEmitsAsyncClickEventsAndBatchFlusherPersistsDailyAggregates() {
        // Step 1: Register User A and create a short URL
        String email = "analytics-user-" + UUID.randomUUID() + "@example.com";
        String token = registerAndGetToken(email);

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);

        String alias = "track-" + UUID.randomUUID().toString().substring(0, 6);
        var createReq =
                new CreateShortUrlRequest("https://example.com/analytics-target", alias, null);
        ResponseEntity<ShortUrlResponse> createRes =
                restTemplate.exchange(
                        "/api/v1/urls",
                        HttpMethod.POST,
                        new HttpEntity<>(createReq, authHeaders),
                        ShortUrlResponse.class);

        assertThat(createRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long linkId = createRes.getBody().id();
        String code = createRes.getBody().code();

        // Step 2: Trigger redirects with various referrers (2 from google.com, 1 from twitter.com,
        // 1 direct)
        sendRedirect(code, "https://www.google.com/search?q=urlbit");
        sendRedirect(code, "https://www.google.com/url?sa=t");
        sendRedirect(code, "https://twitter.com/i/status/123");
        sendRedirect(code, null); // Direct navigation

        // Step 3: Trigger batch flush to aggregate and persist to PostgreSQL
        int flushedCount = clickBatchFlusher.flush();
        assertThat(flushedCount).isGreaterThanOrEqualTo(4);

        // Step 4: Query authenticated analytics endpoint
        ResponseEntity<ShortUrlAnalyticsResponse> analyticsRes =
                restTemplate.exchange(
                        "/api/v1/urls/" + linkId + "/analytics",
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders),
                        ShortUrlAnalyticsResponse.class);

        assertThat(analyticsRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        ShortUrlAnalyticsResponse analytics = analyticsRes.getBody();
        assertThat(analytics).isNotNull();
        assertThat(analytics.shortUrlId()).isEqualTo(linkId);
        assertThat(analytics.code()).isEqualTo(code);
        assertThat(analytics.totalClicks()).isEqualTo(4L);
        assertThat(analytics.deliveryGuarantee()).isEqualTo("best_effort_at_least_once");

        // Verify daily breakdown has today's count = 4
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        var todayCountOpt =
                analytics.dailyClicks().stream().filter(d -> d.day().equals(today)).findFirst();
        assertThat(todayCountOpt).isPresent();
        assertThat(todayCountOpt.get().clicks()).isEqualTo(4L);

        // Verify top referrers
        // google.com (www.google.com) = 2, twitter.com = 1, direct = 1
        assertThat(analytics.topReferrers()).hasSize(3);
        assertThat(analytics.topReferrers().get(0).referrer()).isEqualTo("www.google.com");
        assertThat(analytics.topReferrers().get(0).clicks()).isEqualTo(2L);
    }

    @Test
    void analyticsRequiresAuthentication() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/urls/1/analytics", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void analyticsEnforcesOwnershipIsolation() {
        // User A creates a link
        String tokenA = registerAndGetToken("user-a-" + UUID.randomUUID() + "@example.com");
        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);

        var createReq =
                new CreateShortUrlRequest(
                        "https://example.com/target-a",
                        "iso-" + UUID.randomUUID().toString().substring(0, 6),
                        null);
        ResponseEntity<ShortUrlResponse> createRes =
                restTemplate.exchange(
                        "/api/v1/urls",
                        HttpMethod.POST,
                        new HttpEntity<>(createReq, headersA),
                        ShortUrlResponse.class);
        Long linkIdA = createRes.getBody().id();

        // User B attempts to access User A's analytics -> must return 404
        String tokenB = registerAndGetToken("user-b-" + UUID.randomUUID() + "@example.com");
        HttpHeaders headersB = new HttpHeaders();
        headersB.setBearerAuth(tokenB);

        ResponseEntity<String> idorRes =
                restTemplate.exchange(
                        "/api/v1/urls/" + linkIdA + "/analytics",
                        HttpMethod.GET,
                        new HttpEntity<>(headersB),
                        String.class);

        assertThat(idorRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void analyticsRejectsInvalidDateRanges() {
        String token = registerAndGetToken("daterange-" + UUID.randomUUID() + "@example.com");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        var createReq =
                new CreateShortUrlRequest(
                        "https://example.com/target-date",
                        "date-" + UUID.randomUUID().toString().substring(0, 6),
                        null);
        ResponseEntity<ShortUrlResponse> createRes =
                restTemplate.exchange(
                        "/api/v1/urls",
                        HttpMethod.POST,
                        new HttpEntity<>(createReq, headers),
                        ShortUrlResponse.class);
        Long linkId = createRes.getBody().id();

        // 1. from > to -> 400 Bad Request
        ResponseEntity<String> invertedRes =
                restTemplate.exchange(
                        "/api/v1/urls/" + linkId + "/analytics?from=2026-03-20&to=2026-03-10",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);
        assertThat(invertedRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // 2. range > 90 days -> 400 Bad Request
        ResponseEntity<String> broadRes =
                restTemplate.exchange(
                        "/api/v1/urls/" + linkId + "/analytics?from=2026-01-01&to=2026-06-01",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);
        assertThat(broadRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void sendRedirect(String code, String referer) {
        HttpHeaders headers = new HttpHeaders();
        if (referer != null) {
            headers.add(HttpHeaders.REFERER, referer);
        }
        ResponseEntity<Void> res =
                restTemplate.exchange(
                        "/" + code, HttpMethod.GET, new HttpEntity<>(headers), Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FOUND);
    }

    private String registerAndGetToken(String email) {
        String password = "SecurePassword123!";
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
