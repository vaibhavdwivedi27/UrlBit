package in.urlbit.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class UrlClickDailyId implements Serializable {

    @Column(name = "short_url_id", nullable = false)
    private Long shortUrlId;

    @Column(name = "day", nullable = false)
    private LocalDate day;

    public UrlClickDailyId() {}

    public UrlClickDailyId(Long shortUrlId, LocalDate day) {
        this.shortUrlId = shortUrlId;
        this.day = day;
    }

    public Long getShortUrlId() {
        return shortUrlId;
    }

    public void setShortUrlId(Long shortUrlId) {
        this.shortUrlId = shortUrlId;
    }

    public LocalDate getDay() {
        return day;
    }

    public void setDay(LocalDate day) {
        this.day = day;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UrlClickDailyId that)) return false;
        return Objects.equals(shortUrlId, that.shortUrlId) && Objects.equals(day, that.day);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortUrlId, day);
    }
}
