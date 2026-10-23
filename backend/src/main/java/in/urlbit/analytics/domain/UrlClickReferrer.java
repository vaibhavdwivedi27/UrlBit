package in.urlbit.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "url_click_referrer")
public class UrlClickReferrer {

    @EmbeddedId private UrlClickReferrerId id;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    public UrlClickReferrer() {}

    public UrlClickReferrer(UrlClickReferrerId id, long clickCount) {
        this.id = id;
        this.clickCount = clickCount;
    }

    public UrlClickReferrerId getId() {
        return id;
    }

    public void setId(UrlClickReferrerId id) {
        this.id = id;
    }

    public long getClickCount() {
        return clickCount;
    }

    public void setClickCount(long clickCount) {
        this.clickCount = clickCount;
    }
}
