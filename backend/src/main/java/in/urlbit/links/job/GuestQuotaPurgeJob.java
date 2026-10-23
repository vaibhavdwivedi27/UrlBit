package in.urlbit.links.job;

import in.urlbit.links.service.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to purge guest quota records older than the 30-day cookie lifetime.
 *
 * <p>Since guest cookies have a fixed 30-day Max-Age, purging rows older than 30 days cleans up
 * orphaned state from abandoned cookieless clients without resetting active counters.
 */
@Component
public class GuestQuotaPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(GuestQuotaPurgeJob.class);

    private final LinkService linkService;

    public GuestQuotaPurgeJob(LinkService linkService) {
        this.linkService = linkService;
    }

    @Scheduled(cron = "0 0 3 * * *") // Daily at 03:00 UTC
    public void runPurge() {
        try {
            int purged = linkService.purgeExpiredGuestQuotas();
            log.info("Completed guest quota purge run, removed {} rows", purged);
        } catch (Exception ex) {
            log.error("Failed during scheduled guest quota purge", ex);
        }
    }
}
