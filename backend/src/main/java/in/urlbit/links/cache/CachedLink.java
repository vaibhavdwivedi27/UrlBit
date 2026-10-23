package in.urlbit.links.cache;

import in.urlbit.links.domain.ShortUrl;
import in.urlbit.links.domain.ShortUrlStatus;
import java.time.Instant;

/**
 * Immutable payload cached in Redis under key {@code link:{code}}.
 *
 * <p>Contains link ID, destination URL, lifecycle status, expiration timestamp, and presence flag.
 * Even on cache hit, {@link #isRedirectable(Instant)} validates status and expiration to ensure
 * expired or disabled links are never served.
 */
public record CachedLink(
        Long id, String originalUrl, ShortUrlStatus status, Instant expiresAt, boolean isPresent) {

    public static CachedLink of(ShortUrl entity) {
        return new CachedLink(
                entity.getId(),
                entity.getOriginalUrl(),
                entity.getStatus(),
                entity.getExpiresAt(),
                true);
    }

    public static CachedLink negative() {
        return new CachedLink(null, null, null, null, false);
    }

    public boolean isRedirectable(Instant now) {
        return isPresent
                && status == ShortUrlStatus.ACTIVE
                && (expiresAt == null || expiresAt.isAfter(now));
    }
}
