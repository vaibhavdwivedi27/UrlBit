package in.urlbit.links.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShortUrlStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "disabled_reason", length = 40)
    private DisabledReason disabledReason;

    @Column(name = "last_safety_check_at")
    private Instant lastSafetyCheckAt;

    public ShortUrl() {}

    public ShortUrl(
            UUID ownerId,
            String code,
            String originalUrl,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt,
            ShortUrlStatus status,
            DisabledReason disabledReason,
            Instant lastSafetyCheckAt) {
        this.ownerId = ownerId;
        this.code = code;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.disabledReason = disabledReason;
        this.lastSafetyCheckAt = lastSafetyCheckAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public ShortUrlStatus getStatus() {
        return status;
    }

    public void setStatus(ShortUrlStatus status) {
        this.status = status;
    }

    public DisabledReason getDisabledReason() {
        return disabledReason;
    }

    public void setDisabledReason(DisabledReason disabledReason) {
        this.disabledReason = disabledReason;
    }

    public Instant getLastSafetyCheckAt() {
        return lastSafetyCheckAt;
    }

    public void setLastSafetyCheckAt(Instant lastSafetyCheckAt) {
        this.lastSafetyCheckAt = lastSafetyCheckAt;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    public boolean isRedirectable(Instant now) {
        return status == ShortUrlStatus.ACTIVE && !isExpired(now);
    }
}
