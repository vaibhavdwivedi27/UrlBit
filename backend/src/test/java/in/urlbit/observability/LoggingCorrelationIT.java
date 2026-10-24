package in.urlbit.observability;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.AbstractIntegrationTest;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class LoggingCorrelationIT extends AbstractIntegrationTest {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Pattern UUID_PATTERN =
            Pattern.compile(
                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void incomingRequestIdHeaderIsEchoedInResponse() {
        String customId = "client-trace-" + UUID.randomUUID();
        HttpHeaders headers = new HttpHeaders();
        headers.set(REQUEST_ID_HEADER, customId);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange("/actuator/health", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(REQUEST_ID_HEADER)).isEqualTo(customId);
    }

    @Test
    void missingRequestIdHeaderGeneratesValidUuidInResponse() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String headerValue = response.getHeaders().getFirst(REQUEST_ID_HEADER);
        assertThat(headerValue).isNotNull().isNotBlank();
        assertThat(UUID_PATTERN.matcher(headerValue).matches()).isTrue();
    }

    @Test
    void consecutiveRequestsReceiveDistinctCorrelationIds() {
        ResponseEntity<String> response1 =
                restTemplate.getForEntity("/actuator/health", String.class);
        ResponseEntity<String> response2 =
                restTemplate.getForEntity("/actuator/health", String.class);

        String id1 = response1.getHeaders().getFirst(REQUEST_ID_HEADER);
        String id2 = response2.getHeaders().getFirst(REQUEST_ID_HEADER);

        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();
        assertThat(id1).isNotEqualTo(id2);
    }
}
