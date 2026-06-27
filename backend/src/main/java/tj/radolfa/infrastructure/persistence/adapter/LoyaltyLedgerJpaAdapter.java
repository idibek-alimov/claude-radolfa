package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadLoyaltyLedgerPort;
import tj.radolfa.application.ports.out.SaveLoyaltyLedgerPort;
import tj.radolfa.domain.model.LoyaltyLedgerEntry;
import tj.radolfa.domain.model.LoyaltyReason;
import tj.radolfa.infrastructure.persistence.entity.LoyaltyLedgerEntity;
import tj.radolfa.infrastructure.persistence.repository.LoyaltyLedgerJpaRepository;

import java.time.Instant;
import java.util.List;

@Component
public class LoyaltyLedgerJpaAdapter implements SaveLoyaltyLedgerPort, LoadLoyaltyLedgerPort {

    private final LoyaltyLedgerJpaRepository repository;

    public LoyaltyLedgerJpaAdapter(LoyaltyLedgerJpaRepository repository) {
        this.repository = repository;
    }

    // ── SaveLoyaltyLedgerPort ─────────────────────────────────────────────────

    @Override
    public LoyaltyLedgerEntry append(LoyaltyLedgerEntry entry) {
        LoyaltyLedgerEntity entity = toEntity(entry);
        return toDomain(repository.save(entity));
    }

    @Override
    public void updateRemaining(Long lotId, int newRemaining) {
        repository.updateRemaining(lotId, newRemaining);
    }

    // ── LoadLoyaltyLedgerPort ─────────────────────────────────────────────────

    @Override
    public Page<LoyaltyLedgerEntry> findByUserId(Long userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable).map(this::toDomain);
    }

    @Override
    public List<LoyaltyLedgerEntry> findLiveLots(Long userId) {
        return repository.findLiveLotsByUserIdFifo(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<LoyaltyLedgerEntry> findExpiredLots(Instant now) {
        return repository.findExpiredLots(now)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private LoyaltyLedgerEntity toEntity(LoyaltyLedgerEntry e) {
        return new LoyaltyLedgerEntity(
                e.id(),
                e.userId(),
                e.delta(),
                e.reason(),
                e.orderId(),
                e.actorUserId(),
                e.note(),
                e.sourceLotId(),
                e.remainingPoints(),
                e.expiresAt(),
                e.balanceAfter(),
                e.createdAt());
    }

    private LoyaltyLedgerEntry toDomain(LoyaltyLedgerEntity e) {
        return new LoyaltyLedgerEntry(
                e.getId(),
                e.getUserId(),
                e.getDelta(),
                e.getReason(),
                e.getOrderId(),
                e.getActorUserId(),
                e.getNote(),
                e.getSourceLotId(),
                e.getRemainingPoints(),
                e.getExpiresAt(),
                e.getBalanceAfter(),
                e.getCreatedAt());
    }
}
