package in.urlbit.links.cache;

import in.urlbit.links.event.LinkInvalidatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link LinkInvalidatedEvent} and evicts the corresponding Redis key strictly AFTER
 * transaction commit.
 *
 * <p>Ensures that concurrent reads do not re-populate the cache with stale pre-commit state, and
 * that a rolled-back transaction does not prematurely evict the live cache entry.
 */
@Component
public class LinkCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(LinkCacheInvalidator.class);

    private final LinkCacheService linkCacheService;

    public LinkCacheInvalidator(LinkCacheService linkCacheService) {
        this.linkCacheService = linkCacheService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLinkInvalidated(LinkInvalidatedEvent event) {
        log.debug("Post-commit cache eviction triggered for code '{}'", event.code());
        linkCacheService.evict(event.code());
    }
}
