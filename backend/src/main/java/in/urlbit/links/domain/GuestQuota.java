package in.urlbit.links.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "guest_quotas")
public class GuestQuota {

    public static final short MAX_GUEST_CREATIONS = 5;

    @Id
    @Column(name = "guest_id_hash", length = 128)
    private String guestIdHash;

    @Column(name = "successful_creations", nullable = false)
    private short successfulCreations;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public GuestQuota() {}

    public GuestQuota(
            String guestIdHash, short successfulCreations, Instant createdAt, Instant updatedAt) {
        this.guestIdHash = guestIdHash;
        this.successfulCreations = successfulCreations;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getGuestIdHash() {
        return guestIdHash;
    }

    public void setGuestIdHash(String guestIdHash) {
        this.guestIdHash = guestIdHash;
    }

    public short getSuccessfulCreations() {
        return successfulCreations;
    }

    public void setSuccessfulCreations(short successfulCreations) {
        this.successfulCreations = successfulCreations;
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
}
