package in.urlbit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class UrlbitApplicationIT extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts successfully
        // with Flyway migrations against a real PostgreSQL instance.
    }

    @Test
    void healthEndpointReturns200() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void openApiDocsAvailable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Urlbit API");
    }
}
