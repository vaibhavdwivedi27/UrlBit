package in.urlbit.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "url_click_daily")
public class UrlClickDaily {

    @EmbeddedId private UrlClickDailyId id;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    public UrlClickDaily() {}

    public UrlClickDaily(UrlClickDailyId id, long clickCount) {
        this.id = id;
        this.clickCount = clickCount;
    }

    public UrlClickDailyId getId() {
        return id;
    }

    public void setId(UrlClickDailyId id) {
        this.id = id;
    }

    public long getClickCount() {
        return clickCount;
    }

    public void setClickCount(long clickCount) {
        this.clickCount = clickCount;
    }
}
