package in.urlbit.analytics.repository;

import in.urlbit.analytics.domain.UrlClickDaily;
import in.urlbit.analytics.domain.UrlClickDailyId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UrlClickDailyRepository extends JpaRepository<UrlClickDaily, UrlClickDailyId> {

    @Query(
            "SELECT d FROM UrlClickDaily d "
                    + "WHERE d.id.shortUrlId = :shortUrlId AND d.id.day BETWEEN :from AND :to "
                    + "ORDER BY d.id.day ASC")
    List<UrlClickDaily> findByShortUrlIdAndDayBetween(
            @Param("shortUrlId") Long shortUrlId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
