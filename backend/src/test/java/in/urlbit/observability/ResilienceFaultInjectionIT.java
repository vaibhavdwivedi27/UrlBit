package in.urlbit.observability;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.analytics.event.ClickEvent;
import in.urlbit.analytics.service.AsyncClickEventBuffer;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

class ResilienceFaultInjectionIT extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private ShortUrlRepository shortUrlRepository;

    @Autowired private AsyncClickEventBuffer clickEventBuffer;

    @BeforeEach
    void setUp() {
        var httpClient =
                HttpClients.custom().disableRedirectHandling().disableCookieManagement().build();
        restTemplate
                .getRestTemplate()
                .setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Test
    void redirectSucceedsViaDatabaseFallbackDuringCompleteRedisOutage() {
        String code = "resil-" + UUID.randomUUID().toString().substring(0, 6);
        String destination = "https://example.com/resilience-survivor";

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

        // Pause Redis container to simulate complete network or cache cluster outage
        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            // Hot path HTTP request must gracefully degrade to PostgreSQL and return 302 Found
            ResponseEntity<Void> response = restTemplate.getForEntity("/" + code, Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
            assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo(destination);
        } finally {
            // Always unpause Redis container to maintain isolation for subsequent tests
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
    }

    @Test
    void redirectSucceedsAndDropsEventGracefullyWhenAnalyticsQueueIsSaturated() {
        String code = "drop-" + UUID.randomUUID().toString().substring(0, 6);
        String destination = "https://example.com/drop-test";

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

        // Saturate the bounded in-memory analytics queue to 100% capacity
        int capacity = clickEventBuffer.getCapacity();
        ClickEvent dummy = new ClickEvent(link.getId(), Instant.now(), "example.com");
        for (int i = 0; i < capacity; i++) {
            clickEventBuffer.offer(dummy);
        }

        long initialDrops = clickEventBuffer.getDroppedEventsCount();

        try {
            // Perform redirect request while buffer is completely full
            ResponseEntity<Void> response = restTemplate.getForEntity("/" + code, Void.class);

            // Redirect must NOT fail or throw 500 error; must return 302 Found
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
            assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo(destination);

            // Backpressure policy (OVERFLOW_DROP) must record dropped event count
            assertThat(clickEventBuffer.getDroppedEventsCount()).isGreaterThan(initialDrops);
        } finally {
            // Drain buffer so subsequent tests have a clean queue
            clickEventBuffer.drainBatch(capacity + 50);
        }
    }
}
