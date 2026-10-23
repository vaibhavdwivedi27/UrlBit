package in.urlbit.analytics.service;

import in.urlbit.analytics.domain.UrlClickDaily;
import in.urlbit.analytics.dto.ShortUrlAnalyticsResponse;
import in.urlbit.analytics.dto.ShortUrlAnalyticsResponse.DailyClickCount;
import in.urlbit.analytics.dto.ShortUrlAnalyticsResponse.ReferrerClickCount;
import in.urlbit.analytics.repository.UrlClickDailyRepository;
import in.urlbit.analytics.repository.UrlClickReferrerRepository;
import in.urlbit.common.error.ResourceNotFoundException;
import in.urlbit.links.domain.ShortUrl;
import in.urlbit.links.domain.ShortUrlStatus;
import in.urlbit.links.repository.ShortUrlRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service providing aggregated click reports and top referrer breakdowns.
 *
 * <p>Enforces link ownership verification, date range limits (max 90 days), and zero-fills days
 * with no recorded activity for smooth frontend charting.
 */
@Service
public class AnalyticsService {

    public static final String DELIVERY_GUARANTEE = "best_effort_at_least_once";
    private static final int MAX_DATE_RANGE_DAYS = 90;
    private static final int TOP_REFERRERS_LIMIT = 10;

    private final ShortUrlRepository shortUrlRepository;
    private final UrlClickDailyRepository dailyRepository;
    private final UrlClickReferrerRepository referrerRepository;

    public AnalyticsService(
            ShortUrlRepository shortUrlRepository,
            UrlClickDailyRepository dailyRepository,
            UrlClickReferrerRepository referrerRepository) {
        this.shortUrlRepository = shortUrlRepository;
        this.dailyRepository = dailyRepository;
        this.referrerRepository = referrerRepository;
    }

    @Transactional(readOnly = true)
    public ShortUrlAnalyticsResponse getAnalytics(
            Long shortUrlId, UUID requesterId, LocalDate fromParam, LocalDate toParam) {

        // Step 1: Verify link exists and requester is the authenticated owner
        ShortUrl link =
                shortUrlRepository
                        .findById(shortUrlId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Link not found with id: " + shortUrlId));

        if (link.getStatus() == ShortUrlStatus.DELETED) {
            throw new ResourceNotFoundException("Link not found with id: " + shortUrlId);
        }

        if (link.getOwnerId() == null || !link.getOwnerId().equals(requesterId)) {
            // Return 404 to avoid leaking existence of another user's link
            throw new ResourceNotFoundException("Link not found with id: " + shortUrlId);
        }

        // Step 2: Validate and default date bounds
        LocalDate to = toParam != null ? toParam : LocalDate.now(ZoneOffset.UTC);
        LocalDate from = fromParam != null ? fromParam : to.minusDays(30);

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date cannot be after 'to' date");
        }

        if (ChronoUnit.DAYS.between(from, to) > MAX_DATE_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    "Date range exceeds maximum allowed limit of " + MAX_DATE_RANGE_DAYS + " days");
        }

        // Step 3: Fetch daily counts and zero-fill missing days
        List<UrlClickDaily> dailyRows =
                dailyRepository.findByShortUrlIdAndDayBetween(shortUrlId, from, to);

        Map<LocalDate, Long> countMap =
                dailyRows.stream()
                        .collect(
                                Collectors.toMap(
                                        d -> d.getId().getDay(), UrlClickDaily::getClickCount));

        List<DailyClickCount> dailyClicks = new ArrayList<>();
        long totalClicks = 0;
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            long count = countMap.getOrDefault(cursor, 0L);
            dailyClicks.add(new DailyClickCount(cursor, count));
            totalClicks += count;
            cursor = cursor.plusDays(1);
        }

        // Step 4: Fetch top referrers
        List<UrlClickReferrerRepository.ReferrerSummary> referrerSummaries =
                referrerRepository.findTopReferrers(
                        shortUrlId, from, to, PageRequest.of(0, TOP_REFERRERS_LIMIT));

        List<ReferrerClickCount> topReferrers =
                referrerSummaries.stream()
                        .map(r -> new ReferrerClickCount(r.getReferrer(), r.getTotalClicks()))
                        .toList();

        return new ShortUrlAnalyticsResponse(
                link.getId(),
                link.getCode(),
                from,
                to,
                totalClicks,
                dailyClicks,
                topReferrers,
                DELIVERY_GUARANTEE);
    }
}
