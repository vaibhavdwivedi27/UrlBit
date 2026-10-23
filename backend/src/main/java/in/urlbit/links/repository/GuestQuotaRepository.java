package in.urlbit.links.repository;

import in.urlbit.links.domain.GuestQuota;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestQuotaRepository extends JpaRepository<GuestQuota, String> {

    /**
     * Atomically increments the guest quota up to the maximum limit of 5.
     *
     * <p>If the row does not exist, inserts with successful_creations = 1. If the row exists and
     * successful_creations < 5, increments by 1. If successful_creations >= 5, the WHERE condition
     * fails and RETURNING returns null/empty.
     *
     * <p>This guarantees strict transactional atomicity under concurrent requests without
     * table-level locking.
     */
    @Query(
            value =
                    "INSERT INTO guest_quotas (guest_id_hash, successful_creations, created_at,"
                            + " updated_at) VALUES (:guestIdHash, 1, NOW(), NOW()) ON CONFLICT"
                            + " (guest_id_hash) DO UPDATE SET successful_creations ="
                            + " guest_quotas.successful_creations + 1, updated_at = NOW() WHERE"
                            + " guest_quotas.successful_creations < 5 RETURNING successful_creations",
            nativeQuery = true)
    Short incrementIfUnderQuota(@Param("guestIdHash") String guestIdHash);

    @Modifying
    @Query(value = "DELETE FROM guest_quotas WHERE created_at < :cutoff", nativeQuery = true)
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
