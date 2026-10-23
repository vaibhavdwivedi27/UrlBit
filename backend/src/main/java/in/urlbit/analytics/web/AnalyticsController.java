package in.urlbit.analytics.web;

import in.urlbit.analytics.dto.ShortUrlAnalyticsResponse;
import in.urlbit.analytics.service.AnalyticsService;
import in.urlbit.common.error.UnauthorizedException;
import in.urlbit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls/{id}/analytics")
@Tag(name = "Analytics", description = "Asynchronous link click reporting and metrics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "Get link click analytics with daily aggregates and top referrers")
    public ShortUrlAnalyticsResponse getAnalytics(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required to access link analytics");
        }
        return analyticsService.getAnalytics(id, principal.id(), from, to);
    }
}
