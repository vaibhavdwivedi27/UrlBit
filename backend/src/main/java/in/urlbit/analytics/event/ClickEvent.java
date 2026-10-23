package in.urlbit.analytics.event;

import java.time.Instant;

/**
 * Immutable in-memory event emitted on each redirect.
 *
 * @param shortUrlId Database ID of the resolved short URL
 * @param clickedAt Timestamp when the redirect occurred
 * @param referrerHost Sanitized host from the HTTP Referer header (or "direct")
 */
public record ClickEvent(Long shortUrlId, Instant clickedAt, String referrerHost) {}
