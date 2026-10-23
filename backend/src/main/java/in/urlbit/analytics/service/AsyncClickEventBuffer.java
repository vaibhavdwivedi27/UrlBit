package in.urlbit.analytics.service;

import in.urlbit.analytics.event.ClickEvent;
import in.urlbit.config.AnalyticsProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Bounded in-memory event buffer for click analytics.
 *
 * <p>Enforces an explicit {@code OVERFLOW_DROP} backpressure policy so that redirect latency is
 * never degraded by analytics aggregation or database slowdowns. Exposes Micrometer metrics for
 * queue size and dropped events.
 */
@Component
public class AsyncClickEventBuffer {

    private static final Logger log = LoggerFactory.getLogger(AsyncClickEventBuffer.class);

    private final ArrayBlockingQueue<ClickEvent> queue;
    private final int capacity;
    private final AtomicLong droppedEvents = new AtomicLong(0);
    private final Counter droppedCounter;

    public AsyncClickEventBuffer(AnalyticsProperties properties, MeterRegistry meterRegistry) {
        this.capacity = properties.getQueueCapacity();
        this.queue = new ArrayBlockingQueue<>(capacity);

        this.droppedCounter =
                Counter.builder("analytics.events.dropped")
                        .description("Number of click events dropped due to queue overflow")
                        .register(meterRegistry);

        Gauge.builder("analytics.queue.size", queue, ArrayBlockingQueue::size)
                .description("Current number of buffered click events awaiting batch flush")
                .register(meterRegistry);
    }

    public boolean offer(ClickEvent event) {
        if (event == null) {
            return false;
        }

        boolean accepted = queue.offer(event);
        if (!accepted) {
            droppedEvents.incrementAndGet();
            droppedCounter.increment();
            log.warn(
                    "Analytics queue full (capacity {}). Dropped click event for link {}",
                    capacity,
                    event.shortUrlId());
            return false;
        }
        return true;
    }

    public List<ClickEvent> drainBatch(int maxBatchSize) {
        List<ClickEvent> batch = new ArrayList<>(Math.min(queue.size(), maxBatchSize));
        queue.drainTo(batch, maxBatchSize);
        return batch;
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public long getDroppedEventsCount() {
        return droppedEvents.get();
    }

    public int getCapacity() {
        return capacity;
    }
}
