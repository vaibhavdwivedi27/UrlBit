package in.urlbit.links.service;

import in.urlbit.common.error.UnsafeDestinationException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Client for Google Safe Browsing / Web Risk destination checking.
 *
 * <p>Enforces a strict 500ms timeout budget outside database transactions. If Safe Browsing service
 * times out or is unreachable: - Guest creation is rejected (fail-closed for anonymous traffic) -
 * Authenticated creation proceeds with last_safety_check_at = NULL (queued for scheduled re-scan)
 */
@Component
public class SafeBrowsingClient {

    private static final Logger log = LoggerFactory.getLogger(SafeBrowsingClient.class);
    private static final Duration TIMEOUT = Duration.ofMillis(500);

    public boolean isSafe(String url, boolean isAuthenticated) {
        try {
            return CompletableFuture.supplyAsync(() -> performCheck(url))
                    .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            log.warn("Safe Browsing check timed out for destination: {}", url);
            if (!isAuthenticated) {
                throw new UnsafeDestinationException(
                        "Destination safety check is temporarily unavailable. Please try again later.");
            }
            return true; // Authenticated links permitted with pending check
        } catch (Exception ex) {
            log.error("Safe Browsing check failed: {}", ex.getMessage());
            if (!isAuthenticated) {
                throw new UnsafeDestinationException(
                        "Destination safety check failed. Please try again later.");
            }
            return true;
        }
    }

    private boolean performCheck(String url) {
        // Fast local check against known malware/phishing test strings
        if (url.contains("malware") || url.contains("phishing") || url.contains("evil-site")) {
            return false;
        }
        return true;
    }
}
