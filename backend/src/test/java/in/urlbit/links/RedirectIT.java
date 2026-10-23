package in.urlbit.links;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.links.domain.DisabledReason;
import in.urlbit.links.domain.ShortUrl;
import in.urlbit.links.domain.ShortUrlStatus;
import in.urlbit.links.repository.ShortUrlRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RedirectIT extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private ShortUrlRepository shortUrlRepository;

    @Test
    void activeLinkRedirectsWith302Found() {
        String code = "red302";
        String destination = "https://example.org/destination-target";

        ShortUrl link =
                new ShortUrl(
                        null,
                        code,
                        destination,
                        Instant.now(),
                        Instant.now(),
                        Instant.now().plus(7, ChronoUnit.DAYS),
                        ShortUrlStatus.ACTIVE,
                        null,
                        Instant.now());
        shortUrlRepository.save(link);

        // Disable automatic redirect following to inspect HTTP 302 and Location header
        ResponseEntity<Void> response = restTemplate.getForEntity("/" + code, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo(destination);
    }

    @Test
    void unknownCodeReturns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/unknownCode99", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void disabledLinkReturns404() {
        String code = "dis404";
        ShortUrl link =
                new ShortUrl(
                        null,
                        code,
                        "https://example.org/disabled",
                        Instant.now(),
                        Instant.now(),
                        null,
                        ShortUrlStatus.DISABLED,
                        DisabledReason.OWNER,
                        Instant.now());
        shortUrlRepository.save(link);

        ResponseEntity<String> response = restTemplate.getForEntity("/" + code, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void expiredLinkReturns404() {
        String code = "exp404";
        ShortUrl link =
                new ShortUrl(
                        null,
                        code,
                        "https://example.org/expired",
                        Instant.now().minus(10, ChronoUnit.DAYS),
                        Instant.now().minus(10, ChronoUnit.DAYS),
                        Instant.now().minus(1, ChronoUnit.DAYS), // Expired yesterday
                        ShortUrlStatus.ACTIVE,
                        null,
                        Instant.now());
        shortUrlRepository.save(link);

        ResponseEntity<String> response = restTemplate.getForEntity("/" + code, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
