package in.urlbit.links.job;

import in.urlbit.links.service.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to periodically re-scan active destination URLs against Google Safe Browsing.
 *
 * <p>A destination can turn malicious after creation (compromised domain, new redirect). This job
 * claims batches of links using {@code FOR UPDATE SKIP LOCKED} so concurrent instances never scan
 * the same link, marks flagged links with {@code ABUSE_TAKEDOWN}, and invalidates them from Redis.
 */
@Component
public class SafeBrowsingRescanJob {

    private static final Logger log = LoggerFactory.getLogger(SafeBrowsingRescanJob.class);
    private static final int BATCH_SIZE = 50;

    private final LinkService linkService;

    public SafeBrowsingRescanJob(LinkService linkService) {
        this.linkService = linkService;
    }

    @Scheduled(cron = "${urlbit.jobs.rescan.cron:0 0 * * * *}") // Hourly
    public void runRescan() {
        org.slf4j.MDC.put("requestId", "rescan-" + java.util.UUID.randomUUID());
        try {
            int flagged = linkService.rescanBatch(BATCH_SIZE);
            log.info("Completed safety re-scan batch, flagged and disabled {} links", flagged);
        } catch (Exception ex) {
            log.error("Failed during scheduled safety re-scan run", ex);
        } finally {
            org.slf4j.MDC.remove("requestId");
        }
    }
}
