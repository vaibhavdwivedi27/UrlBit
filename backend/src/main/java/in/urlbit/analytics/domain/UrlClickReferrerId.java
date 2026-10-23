package in.urlbit.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class UrlClickReferrerId implements Serializable {

    @Column(name = "short_url_id", nullable = false)
    private Long shortUrlId;

    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "referrer_host", nullable = false, length = 255)
    private String referrerHost;

    public UrlClickReferrerId() {}

    public UrlClickReferrerId(Long shortUrlId, LocalDate day, String referrerHost) {
        this.shortUrlId = shortUrlId;
        this.day = day;
        this.referrerHost = referrerHost;
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

    public String getReferrerHost() {
        return referrerHost;
    }

    public void setReferrerHost(String referrerHost) {
        this.referrerHost = referrerHost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UrlClickReferrerId that)) return false;
        return Objects.equals(shortUrlId, that.shortUrlId)
                && Objects.equals(day, that.day)
                && Objects.equals(referrerHost, that.referrerHost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortUrlId, day, referrerHost);
    }
}
