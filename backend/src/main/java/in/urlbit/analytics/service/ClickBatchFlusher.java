package in.urlbit.analytics.service;

import in.urlbit.analytics.event.ClickEvent;
import in.urlbit.config.AnalyticsProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Background batch worker that drains buffered click events and flushes aggregated counts to
 * PostgreSQL.
 *
 * <p>Aggregates events in-memory per {@code (short_url_id, day)} and {@code (short_url_id, day,
 * referrer_host)} before executing atomic native upserts via {@link JdbcTemplate#batchUpdate}.
 * Never updates the {@code short_urls} row directly, avoiding row lock contention under high
 * redirect traffic.
 */
@Component
public class ClickBatchFlusher {

    private static final Logger log = LoggerFactory.getLogger(ClickBatchFlusher.class);

    private static final String UPSERT_DAILY_SQL =
            "INSERT INTO url_click_daily (short_url_id, day, click_count) "
                    + "VALUES (?, ?, ?) "
                    + "ON CONFLICT (short_url_id, day) "
                    + "DO UPDATE SET click_count = url_click_daily.click_count + EXCLUDED.click_count";

    private static final String UPSERT_REFERRER_SQL =
            "INSERT INTO url_click_referrer (short_url_id, day, referrer_host, click_count) "
                    + "VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT (short_url_id, day, referrer_host) "
                    + "DO UPDATE SET click_count = url_click_referrer.click_count + EXCLUDED.click_count";

    private final AsyncClickEventBuffer buffer;
    private final JdbcTemplate jdbcTemplate;
    private final AnalyticsProperties properties;
    private final Counter processedCounter;

    public ClickBatchFlusher(
            AsyncClickEventBuffer buffer,
            JdbcTemplate jdbcTemplate,
            AnalyticsProperties properties,
            MeterRegistry meterRegistry) {
        this.buffer = buffer;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;

        this.processedCounter =
                Counter.builder("analytics.events.processed")
                        .description("Number of click events successfully aggregated and flushed")
                        .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${urlbit.analytics.flush-interval-ms:1000}")
    public void scheduledFlush() {
        flush();
    }

    public synchronized int flush() {
        if (buffer.isEmpty()) {
            return 0;
        }

        int totalFlushed = 0;
        int batchSize = properties.getBatchSize();

        while (!buffer.isEmpty()) {
            List<ClickEvent> batch = buffer.drainBatch(batchSize);
            if (batch.isEmpty()) {
                break;
            }

            try {
                flushBatch(batch);
                totalFlushed += batch.size();
                processedCounter.increment(batch.size());
            } catch (Exception ex) {
                log.error("Failed to flush batch of {} click events", batch.size(), ex);
                // Under the spec's best-effort contract, continue rather than hanging
                break;
            }
        }

        if (totalFlushed > 0) {
            log.debug("Successfully flushed {} click events to PostgreSQL", totalFlushed);
        }
        return totalFlushed;
    }

    @Transactional
    public void flushBatch(List<ClickEvent> batch) {
        Map<DailyKey, Long> dailyCounts = new HashMap<>();
        Map<ReferrerKey, Long> referrerCounts = new HashMap<>();

        for (ClickEvent event : batch) {
            LocalDate day = event.clickedAt().atZone(ZoneOffset.UTC).toLocalDate();
            dailyCounts.merge(new DailyKey(event.shortUrlId(), day), 1L, Long::sum);
            referrerCounts.merge(
                    new ReferrerKey(event.shortUrlId(), day, event.referrerHost()), 1L, Long::sum);
        }

        // Batch upsert url_click_daily
        List<Object[]> dailyArgs =
                dailyCounts.entrySet().stream()
                        .map(
                                entry ->
                                        new Object[] {
                                            entry.getKey().shortUrlId(),
                                            entry.getKey().day(),
                                            entry.getValue()
                                        })
                        .toList();

        jdbcTemplate.batchUpdate(UPSERT_DAILY_SQL, dailyArgs);

        // Batch upsert url_click_referrer
        List<Object[]> referrerArgs =
                referrerCounts.entrySet().stream()
                        .map(
                                entry ->
                                        new Object[] {
                                            entry.getKey().shortUrlId(),
                                            entry.getKey().day(),
                                            entry.getKey().referrerHost(),
                                            entry.getValue()
                                        })
                        .toList();

        jdbcTemplate.batchUpdate(UPSERT_REFERRER_SQL, referrerArgs);
    }

    @PreDestroy
    public void onShutdown() {
        int remaining = buffer.size();
        if (remaining > 0) {
            log.info("Application shutting down: flushing {} buffered click events", remaining);
            flush();
        }
    }

    private record DailyKey(Long shortUrlId, LocalDate day) {}

    private record ReferrerKey(Long shortUrlId, LocalDate day, String referrerHost) {}
}
