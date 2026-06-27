package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.LoyaltyLedgerEntity;

import java.time.Instant;
import java.util.List;

public interface LoyaltyLedgerJpaRepository extends JpaRepository<LoyaltyLedgerEntity, Long> {

    /** Ops drawer: all movements for a user, sorted by the supplied pageable. */
    Page<LoyaltyLedgerEntity> findByUserId(Long userId, Pageable pageable);

    /**
     * FIFO live-lot scan: credit rows with remaining points, oldest first.
     * Matches the partial index {@code idx_loyalty_ledger_live_lots}.
     */
    @Query("SELECT e FROM LoyaltyLedgerEntity e " +
           "WHERE e.userId = :userId AND e.delta > 0 AND e.remainingPoints > 0 " +
           "ORDER BY e.createdAt ASC")
    List<LoyaltyLedgerEntity> findLiveLotsByUserIdFifo(@Param("userId") Long userId);

    /**
     * Expiry sweep: live lots whose expires_at is before the given instant.
     * Matches the partial index {@code idx_loyalty_ledger_expiry}.
     */
    @Query("SELECT e FROM LoyaltyLedgerEntity e " +
           "WHERE e.delta > 0 AND e.remainingPoints > 0 " +
           "  AND e.expiresAt IS NOT NULL AND e.expiresAt < :now")
    List<LoyaltyLedgerEntity> findExpiredLots(@Param("now") Instant now);

    /** In-place decrement of remaining_points on a credit lot. */
    @Modifying
    @Query("UPDATE LoyaltyLedgerEntity e SET e.remainingPoints = :newRemaining WHERE e.id = :id")
    void updateRemaining(@Param("id") Long id, @Param("newRemaining") int newRemaining);
}
