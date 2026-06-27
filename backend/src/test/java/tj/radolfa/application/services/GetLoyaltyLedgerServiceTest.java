package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tj.radolfa.application.ports.out.LoadLoyaltyLedgerPort;
import tj.radolfa.application.ports.out.SaveLoyaltyLedgerPort;
import tj.radolfa.domain.model.LoyaltyLedgerEntry;
import tj.radolfa.domain.model.LoyaltyReason;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetLoyaltyLedgerServiceTest {

    // ── Fakes ────────────────────────────────────────────────────────────────

    static class FakeLedgerPort implements LoadLoyaltyLedgerPort, SaveLoyaltyLedgerPort {
        Long capturedUserId;
        Pageable capturedPageable;
        Page<LoyaltyLedgerEntry> pageToReturn = Page.empty();

        @Override
        public Page<LoyaltyLedgerEntry> findByUserId(Long userId, Pageable pageable) {
            capturedUserId   = userId;
            capturedPageable = pageable;
            return pageToReturn;
        }

        @Override public List<LoyaltyLedgerEntry> findLiveLots(Long userId) { return List.of(); }
        @Override public List<LoyaltyLedgerEntry> findExpiredLots(Instant now) { return List.of(); }
        @Override public LoyaltyLedgerEntry append(LoyaltyLedgerEntry e) { return e; }
        @Override public void updateRemaining(Long lotId, int newRemaining) {}
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Delegates to LoadLoyaltyLedgerPort with correct userId and Pageable")
    void execute_delegatesToPort() {
        var port    = new FakeLedgerPort();
        var service = new GetLoyaltyLedgerService(port);
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        service.execute(7L, pageable);

        assertEquals(7L, port.capturedUserId);
        assertEquals(pageable, port.capturedPageable);
    }

    @Test
    @DisplayName("Returns the page produced by the port")
    void execute_returnsPageFromPort() {
        var port    = new FakeLedgerPort();
        var service = new GetLoyaltyLedgerService(port);

        LoyaltyLedgerEntry row = new LoyaltyLedgerEntry(
                1L, 7L, 100, LoyaltyReason.EARN_CASHBACK,
                null, null, null, 100, null, 100, Instant.now());
        port.pageToReturn = new PageImpl<>(List.of(row));

        Page<LoyaltyLedgerEntry> result = service.execute(7L, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals(row, result.getContent().get(0));
    }

    @Test
    @DisplayName("Returns empty page when the user has no ledger entries")
    void execute_emptyPage_whenNoEntries() {
        var port    = new FakeLedgerPort();
        var service = new GetLoyaltyLedgerService(port);
        port.pageToReturn = Page.empty();

        Page<LoyaltyLedgerEntry> result = service.execute(99L, Pageable.unpaged());

        assertTrue(result.isEmpty());
    }
}
