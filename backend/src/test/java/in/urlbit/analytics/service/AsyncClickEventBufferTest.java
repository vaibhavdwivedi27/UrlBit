package in.urlbit.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.analytics.event.ClickEvent;
import in.urlbit.config.AnalyticsProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsyncClickEventBufferTest {

    private AsyncClickEventBuffer buffer;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setQueueCapacity(5); // Small capacity for deterministic overflow test
        buffer = new AsyncClickEventBuffer(properties, meterRegistry);
    }

    @Test
    void offerWithinCapacitySucceeds() {
        for (int i = 1; i <= 5; i++) {
            boolean accepted = buffer.offer(new ClickEvent((long) i, Instant.now(), "direct"));
            assertThat(accepted).isTrue();
        }

        assertThat(buffer.size()).isEqualTo(5);
        assertThat(buffer.getDroppedEventsCount()).isZero();
    }

    @Test
    void overflowDropsEventsAndIncrementsCountersWithoutThrowing() {
        // Fill to capacity (5)
        for (int i = 1; i <= 5; i++) {
            buffer.offer(new ClickEvent((long) i, Instant.now(), "direct"));
        }

        // 6th and 7th items overflow
        boolean accepted6 = buffer.offer(new ClickEvent(6L, Instant.now(), "overflow.com"));
        boolean accepted7 = buffer.offer(new ClickEvent(7L, Instant.now(), "overflow.com"));

        assertThat(accepted6).isFalse();
        assertThat(accepted7).isFalse();
        assertThat(buffer.size()).isEqualTo(5);
        assertThat(buffer.getDroppedEventsCount()).isEqualTo(2);

        // Verify Micrometer metric
        double droppedMetric = meterRegistry.get("analytics.events.dropped").counter().count();
        assertThat(droppedMetric).isEqualTo(2.0);
    }

    @Test
    void drainBatchRetrievesLimitedItems() {
        for (int i = 1; i <= 5; i++) {
            buffer.offer(new ClickEvent((long) i, Instant.now(), "direct"));
        }

        List<ClickEvent> batch1 = buffer.drainBatch(3);
        assertThat(batch1).hasSize(3);
        assertThat(buffer.size()).isEqualTo(2);

        List<ClickEvent> batch2 = buffer.drainBatch(10);
        assertThat(batch2).hasSize(2);
        assertThat(buffer.isEmpty()).isTrue();
    }

    @Test
    void nullEventReturnsFalseSafely() {
        assertThat(buffer.offer(null)).isFalse();
        assertThat(buffer.size()).isZero();
    }
}
