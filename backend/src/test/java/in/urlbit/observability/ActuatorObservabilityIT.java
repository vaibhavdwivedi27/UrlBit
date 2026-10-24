package in.urlbit.observability;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.links.domain.ShortUrl;
import in.urlbit.links.domain.ShortUrlStatus;
import in.urlbit.links.repository.ShortUrlRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

class ActuatorObservabilityIT extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private ShortUrlRepository shortUrlRepository;

    @BeforeEach
    void setUp() {
        var httpClient =
                HttpClients.custom().disableRedirectHandling().disableCookieManagement().build();
        restTemplate
                .getRestTemplate()
                .setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Test
    void healthEndpointReturnsUpWithComponents() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
        assertThat(response.getBody()).contains("\"db\"");
        assertThat(response.getBody()).contains("\"redis\"");
    }

    @Test
    void livenessProbeReturnsUp() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health/liveness", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void readinessProbeReturnsUpWithDatabaseAndRedis() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health/readiness", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
        assertThat(response.getBody()).contains("\"db\"");
        assertThat(response.getBody()).contains("\"redis\"");
    }

    @Test
    void prometheusEndpointExposesMetricsAndLowCardinalityTags() {
        String testCode = "obs" + UUID.randomUUID().toString().substring(0, 4);
        ShortUrl link =
                new ShortUrl(
                        null,
                        testCode,
                        "https://example.com/actuator-test",
                        Instant.now(),
                        Instant.now(),
                        Instant.now().plus(7, ChronoUnit.DAYS),
                        ShortUrlStatus.ACTIVE,
                        null,
                        Instant.now());
        shortUrlRepository.save(link);

        // Perform redirect request to record HTTP server metrics
        ResponseEntity<Void> redirectResponse =
                restTemplate.getForEntity("/" + testCode, Void.class);
        assertThat(redirectResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        ResponseEntity<String> prometheusResponse =
                restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(prometheusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = prometheusResponse.getBody();
        assertThat(body).isNotNull();

        // Verify global application tag configured in application.yml
        assertThat(body).contains("application=\"urlbit\"");

        // Verify HTTP request metrics and percentiles (p50, p95, p99)
        assertThat(body).contains("http_server_requests_seconds");
        assertThat(body).contains("quantile=\"0.95\"");
        assertThat(body).contains("quantile=\"0.99\"");

        // Verify low-cardinality constraint: HTTP server request metrics must use route template,
        // not raw short code
        assertThat(body).contains("uri=\"/{code:[a-zA-Z0-9_-]+}\"");
        var serverRequestLines =
                body.lines().filter(l -> l.startsWith("http_server_requests_seconds")).toList();
        assertThat(serverRequestLines).isNotEmpty();
        assertThat(serverRequestLines).allMatch(l -> !l.contains("/" + testCode));

        // Verify HikariCP database pool metrics
        assertThat(body).contains("hikaricp_connections");

        // Verify custom Micrometer counters for analytics
        assertThat(body).contains("analytics_events_");
    }
}
