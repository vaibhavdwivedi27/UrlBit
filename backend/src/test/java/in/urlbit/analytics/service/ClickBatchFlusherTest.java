package in.urlbit.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import in.urlbit.analytics.event.ClickEvent;
import in.urlbit.config.AnalyticsProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ClickBatchFlusherTest {

    @Mock private JdbcTemplate jdbcTemplate;

    private AsyncClickEventBuffer buffer;
    private ClickBatchFlusher flusher;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setQueueCapacity(100);
        properties.setBatchSize(50);
        buffer = new AsyncClickEventBuffer(properties, meterRegistry);
        flusher = new ClickBatchFlusher(buffer, jdbcTemplate, properties, meterRegistry);
    }

    @Test
    void flushAggregatesMultipleClicksPerDayAndReferrer() {
        Instant now = Instant.parse("2026-03-15T10:00:00Z");
        LocalDate expectedDay = now.atZone(ZoneOffset.UTC).toLocalDate();

        // 3 clicks for link 1 with referrer google.com
        buffer.offer(new ClickEvent(1L, now, "google.com"));
        buffer.offer(new ClickEvent(1L, now.plusSeconds(10), "google.com"));
        buffer.offer(new ClickEvent(1L, now.plusSeconds(20), "google.com"));

        // 2 clicks for link 1 with referrer twitter.com
        buffer.offer(new ClickEvent(1L, now.plusSeconds(30), "twitter.com"));
        buffer.offer(new ClickEvent(1L, now.plusSeconds(40), "twitter.com"));

        // 1 click for link 2 with direct
        buffer.offer(new ClickEvent(2L, now.plusSeconds(50), "direct"));

        int flushed = flusher.flush();
        assertThat(flushed).isEqualTo(6);
        assertThat(buffer.isEmpty()).isTrue();

        // Capture arguments sent to jdbcTemplate.batchUpdate for url_click_daily
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> dailyCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate)
                .batchUpdate(
                        eq(
                                "INSERT INTO url_click_daily (short_url_id, day, click_count) "
                                        + "VALUES (?, ?, ?) "
                                        + "ON CONFLICT (short_url_id, day) "
                                        + "DO UPDATE SET click_count = url_click_daily.click_count + EXCLUDED.click_count"),
                        dailyCaptor.capture());

        List<Object[]> dailyRows = dailyCaptor.getValue();
        // Link 1 (total 5 clicks) and Link 2 (total 1 click) -> exactly 2 aggregated rows
        assertThat(dailyRows).hasSize(2);

        // Capture arguments sent to jdbcTemplate.batchUpdate for url_click_referrer
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> referrerCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate)
                .batchUpdate(
                        eq(
                                "INSERT INTO url_click_referrer (short_url_id, day, referrer_host, click_count) "
                                        + "VALUES (?, ?, ?, ?) "
                                        + "ON CONFLICT (short_url_id, day, referrer_host) "
                                        + "DO UPDATE SET click_count = url_click_referrer.click_count + EXCLUDED.click_count"),
                        referrerCaptor.capture());

        List<Object[]> referrerRows = referrerCaptor.getValue();
        // Link 1 (google.com: 3, twitter.com: 2) and Link 2 (direct: 1) -> exactly 3 rows
        assertThat(referrerRows).hasSize(3);
    }
}
