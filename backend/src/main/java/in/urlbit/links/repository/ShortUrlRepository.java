package in.urlbit.links.repository;

import in.urlbit.links.domain.ShortUrl;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByCode(String code);

    Page<ShortUrl> findByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Inserts a short URL row using PostgreSQL native ON CONFLICT DO NOTHING.
     *
     * <p>Returns the generated ID if inserted, or null/empty if a unique code collision occurred.
     * This avoids aborting the PostgreSQL transaction on collision so caller state (such as the
     * guest quota counter) is not lost.
     */
    @Query(
            value =
                    "INSERT INTO short_urls (owner_id, code, original_url, created_at, updated_at,"
                            + " expires_at, status, disabled_reason, last_safety_check_at) VALUES"
                            + " (:ownerId, :code, :originalUrl, :createdAt, :updatedAt, :expiresAt,"
                            + " :status, :disabledReason, :lastSafetyCheckAt) ON CONFLICT (code) DO"
                            + " NOTHING RETURNING id",
            nativeQuery = true)
    Long insertOnConflictDoNothing(
            @Param("ownerId") UUID ownerId,
            @Param("code") String code,
            @Param("originalUrl") String originalUrl,
            @Param("createdAt") Instant createdAt,
            @Param("updatedAt") Instant updatedAt,
            @Param("expiresAt") Instant expiresAt,
            @Param("status") String status,
            @Param("disabledReason") String disabledReason,
            @Param("lastSafetyCheckAt") Instant lastSafetyCheckAt);
}
