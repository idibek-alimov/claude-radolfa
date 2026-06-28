package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.ExpireOrderUseCase;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.SavePaymentPort;
import tj.radolfa.application.ports.out.SavePaymentStatusChangePort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Payment;
import tj.radolfa.domain.model.PaymentStatus;
import tj.radolfa.domain.model.PaymentStatusChange;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FailPaymentServiceTest {

    static final String TX_ID    = "TX-001";
    static final Long   ORDER_ID = 1L;
    static final Long   PAY_ID   = 5L;

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeSavePaymentStatusChangePort implements SavePaymentStatusChangePort {
        final List<PaymentStatusChange> appended = new ArrayList<>();
        @Override public PaymentStatusChange append(PaymentStatusChange c) { appended.add(c); return c; }
    }

    static PaymentStatusChangeRecorder noopRecorder() {
        return new PaymentStatusChangeRecorder(new FakeSavePaymentStatusChangePort());
    }

    static Payment payment(PaymentStatus status) {
        return new Payment(PAY_ID, ORDER_ID, Money.of(BigDecimal.valueOf(100)), "TJS",
                status, "payme", TX_ID, null, Instant.now(), null);
    }

    static LoadPaymentPort loadPort(Payment p) {
        return new LoadPaymentPort() {
            @Override public Optional<Payment> findByOrderId(Long id) { return Optional.empty(); }
            @Override public Optional<Payment> findByProviderTransactionId(String tx) {
                return Optional.ofNullable(tx.equals(TX_ID) ? p : null);
            }
        };
    }

    static class CapturingSavePaymentPort implements SavePaymentPort {
        final List<Payment> saved = new ArrayList<>();
        @Override public Payment save(Payment p) { saved.add(p); return p; }
    }

    static ExpireOrderUseCase noopExpire() {
        return (orderId, reason) -> {};
    }

    static FailPaymentService service(LoadPaymentPort lp, SavePaymentPort sp,
                                       PaymentStatusChangeRecorder recorder) {
        return new FailPaymentService(lp, sp, noopExpire(), recorder);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROCESSING → FAILED records one ledger row with null actor")
    void processingToFailed_recordsLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();

        service(loadPort(payment(PaymentStatus.PROCESSING)), savePort, recorder).execute(TX_ID);

        assertEquals(1, savePort.saved.size());
        assertEquals(PaymentStatus.FAILED, savePort.saved.get(0).status());

        assertEquals(1, ledgerPort.appended.size());
        var row = ledgerPort.appended.get(0);
        assertEquals(PaymentStatus.PROCESSING, row.statusFrom());
        assertEquals(PaymentStatus.FAILED,     row.statusTo());
        assertNull(row.actorUserId());
    }

    @Test
    @DisplayName("Already FAILED → idempotency guard, no save, no ledger row")
    void alreadyFailed_idempotent_noLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();

        service(loadPort(payment(PaymentStatus.FAILED)), savePort, recorder).execute(TX_ID);

        assertTrue(savePort.saved.isEmpty());
        assertTrue(ledgerPort.appended.isEmpty());
    }

    @Test
    @DisplayName("Already COMPLETED → idempotency guard (warn+skip), no save, no ledger row")
    void alreadyCompleted_skip_noLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();

        service(loadPort(payment(PaymentStatus.COMPLETED)), savePort, recorder).execute(TX_ID);

        assertTrue(savePort.saved.isEmpty());
        assertTrue(ledgerPort.appended.isEmpty());
    }
}
