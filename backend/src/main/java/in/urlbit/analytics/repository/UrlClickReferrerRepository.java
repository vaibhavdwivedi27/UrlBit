package in.urlbit.analytics.repository;

import in.urlbit.analytics.domain.UrlClickReferrer;
import in.urlbit.analytics.domain.UrlClickReferrerId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UrlClickReferrerRepository
        extends JpaRepository<UrlClickReferrer, UrlClickReferrerId> {

    @Query(
            "SELECT r.id.referrerHost AS referrer, SUM(r.clickCount) AS totalClicks "
                    + "FROM UrlClickReferrer r "
                    + "WHERE r.id.shortUrlId = :shortUrlId AND r.id.day BETWEEN :from AND :to "
                    + "GROUP BY r.id.referrerHost "
                    + "ORDER BY SUM(r.clickCount) DESC")
    List<ReferrerSummary> findTopReferrers(
            @Param("shortUrlId") Long shortUrlId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    interface ReferrerSummary {
        String getReferrer();

        long getTotalClicks();
    }
}
