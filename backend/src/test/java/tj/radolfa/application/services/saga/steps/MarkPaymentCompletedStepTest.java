package tj.radolfa.application.services.saga.steps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.ProcessRefundPort;
import tj.radolfa.application.ports.out.SavePaymentPort;
import tj.radolfa.application.ports.out.SavePaymentStatusChangePort;
import tj.radolfa.application.services.PaymentStatusChangeRecorder;
import tj.radolfa.application.services.saga.PaymentConfirmationContext;
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

class MarkPaymentCompletedStepTest {

    static final String TX_ID      = "TX-001";
    static final Long   PAYMENT_ID = 7L;
    static final Long   ORDER_ID   = 1L;

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeSavePaymentStatusChangePort implements SavePaymentStatusChangePort {
        final List<PaymentStatusChange> appended = new ArrayList<>();
        @Override public PaymentStatusChange append(PaymentStatusChange c) { appended.add(c); return c; }
    }

    static PaymentStatusChangeRecorder noopRecorder() {
        return new PaymentStatusChangeRecorder(new FakeSavePaymentStatusChangePort());
    }

    static Payment processingPayment() {
        return new Payment(PAYMENT_ID, ORDER_ID, Money.of(BigDecimal.valueOf(100)), "TJS",
                PaymentStatus.PROCESSING, "payme", TX_ID, null, Instant.now(), null);
    }

    static Payment completedPayment() {
        return processingPayment().completed(TX_ID);
    }

    static LoadPaymentPort loadPort(Payment p) {
        return new LoadPaymentPort() {
            @Override public Optional<Payment> findByOrderId(Long id) { return Optional.empty(); }
            @Override public Optional<Payment> findByProviderTransactionId(String tx) {
                return tx.equals(p.providerTransactionId()) ? Optional.of(p) : Optional.empty();
            }
        };
    }

    static class CapturingSavePaymentPort implements SavePaymentPort {
        final List<Payment> saved = new ArrayList<>();
        @Override public Payment save(Payment p) { saved.add(p); return p; }
    }

    static ProcessRefundPort noopRefundPort() {
        return (orderId, txId, amount) -> new ProcessRefundPort.RefundResult(true, "REFUND-X", null);
    }

    static MarkPaymentCompletedStep step(LoadPaymentPort lp, SavePaymentPort sp,
                                          PaymentStatusChangeRecorder recorder) {
        return new MarkPaymentCompletedStep(lp, sp, noopRefundPort(), recorder);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("execute: PROCESSING → COMPLETED records ledger row with null actor")
    void execute_recordsLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();
        var ctx        = new PaymentConfirmationContext(TX_ID);

        step(loadPort(processingPayment()), savePort, recorder).execute(ctx);

        assertNotNull(ctx.payment);
        assertEquals(PaymentStatus.COMPLETED, ctx.payment.status());
        assertEquals(1, ledgerPort.appended.size());
        var row = ledgerPort.appended.get(0);
        assertEquals(PaymentStatus.PROCESSING, row.statusFrom());
        assertEquals(PaymentStatus.COMPLETED,  row.statusTo());
        assertNull(row.actorUserId());
    }

    @Test
    @DisplayName("execute: already COMPLETED → no save, no ledger row (idempotent)")
    void execute_alreadyCompleted_idempotent() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();
        var ctx        = new PaymentConfirmationContext(TX_ID);

        step(loadPort(completedPayment()), savePort, recorder).execute(ctx);

        assertTrue(savePort.saved.isEmpty());
        assertTrue(ledgerPort.appended.isEmpty());
    }

    @Test
    @DisplayName("compensate: COMPLETED → FAILED records ledger row with null actor")
    void compensate_recordsLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();
        var ctx        = new PaymentConfirmationContext(TX_ID);
        ctx.payment    = completedPayment();

        step(loadPort(completedPayment()), savePort, recorder).compensate(ctx);

        assertEquals(1, ledgerPort.appended.size());
        var row = ledgerPort.appended.get(0);
        assertEquals(PaymentStatus.COMPLETED, row.statusFrom());
        assertEquals(PaymentStatus.FAILED,    row.statusTo());
        assertNull(row.actorUserId());
    }

    @Test
    @DisplayName("compensate: ctx.payment is null → no save, no ledger row")
    void compensate_nullPayment_noLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();
        var ctx        = new PaymentConfirmationContext(TX_ID);

        step(loadPort(processingPayment()), savePort, recorder).compensate(ctx);

        assertTrue(savePort.saved.isEmpty());
        assertTrue(ledgerPort.appended.isEmpty());
    }
}
