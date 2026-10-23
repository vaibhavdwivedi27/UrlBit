package in.urlbit.analytics.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated click reporting response DTO for an authenticated short URL.
 *
 * <p>Includes total clicks in the requested range, daily time-series counts, top referrer hosts,
 * and delivery guarantee disclaimer.
 */
public record ShortUrlAnalyticsResponse(
        Long shortUrlId,
        String code,
        LocalDate from,
        LocalDate to,
        long totalClicks,
        List<DailyClickCount> dailyClicks,
        List<ReferrerClickCount> topReferrers,
        String deliveryGuarantee) {

    public record DailyClickCount(LocalDate day, long clicks) {}

    public record ReferrerClickCount(String referrer, long clicks) {}
}
