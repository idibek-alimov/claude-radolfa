package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.payment.InitiatePaymentUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.PaymentPort;
import tj.radolfa.application.ports.out.SavePaymentPort;
import tj.radolfa.application.ports.out.SavePaymentStatusChangePort;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.Payment;
import tj.radolfa.domain.model.PaymentStatus;
import tj.radolfa.domain.model.PaymentStatusChange;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class InitiatePaymentServiceTest {

    static final Long ORDER_ID = 1L;
    static final Long USER_ID  = 10L;

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeSavePaymentStatusChangePort implements SavePaymentStatusChangePort {
        final List<PaymentStatusChange> appended = new ArrayList<>();
        @Override public PaymentStatusChange append(PaymentStatusChange c) { appended.add(c); return c; }
    }

    static PaymentStatusChangeRecorder noopRecorder() {
        return new PaymentStatusChangeRecorder(new FakeSavePaymentStatusChangePort());
    }

    static LoadOrderPort orderPort(Order o) {
        return new LoadOrderPort() {
            @Override public Optional<Order> loadById(Long id) { return id.equals(ORDER_ID) ? Optional.of(o) : Optional.empty(); }
            @Override public List<Order> loadByUserId(Long u) { return List.of(); }
            @Override public Optional<Order> loadByExternalOrderId(String e) { return Optional.empty(); }
            @Override public List<Order> loadRecentPaidByUserId(Long u, int l) { return List.of(); }
        };
    }

    static LoadPaymentPort noPaymentPort() {
        return new LoadPaymentPort() {
            @Override public Optional<Payment> findByOrderId(Long id) { return Optional.empty(); }
            @Override public Optional<Payment> findByProviderTransactionId(String tx) { return Optional.empty(); }
        };
    }

    static class CapturingSavePaymentPort implements SavePaymentPort {
        final AtomicLong idSeq = new AtomicLong(1);
        final List<Payment> saved = new ArrayList<>();
        @Override public Payment save(Payment p) {
            var withId = new Payment(idSeq.getAndIncrement(), p.orderId(), p.amount(), p.currency(),
                    p.status(), p.provider(), p.providerTransactionId(), p.providerRedirectUrl(),
                    p.createdAt(), p.completedAt());
            saved.add(withId);
            return withId;
        }
    }

    static PaymentPort gatewayPort(String txId, String redirectUrl) {
        return new PaymentPort() {
            @Override public PaymentIntent initiate(Money amount, String currency, String orderId, String userId) {
                return new PaymentIntent(txId, redirectUrl, null);
            }
            @Override public RefundResult refund(String providerTxId, Money amount) {
                return new RefundResult("REFUND-X", true, null);
            }
        };
    }

    static Order pendingOrder() {
        return new Order.Builder()
                .id(ORDER_ID).userId(USER_ID).status(OrderStatus.PENDING)
                .deliveryType(DeliveryType.HOME)
                .totalAmount(Money.of(BigDecimal.valueOf(100)))
                .createdAt(Instant.now())
                .items(List.of())
                .build();
    }

    static InitiatePaymentService service(LoadOrderPort op, LoadPaymentPort pp,
                                           CapturingSavePaymentPort sp, PaymentPort gp,
                                           PaymentStatusChangeRecorder recorder) {
        return new InitiatePaymentService(op, pp, sp, gp, recorder);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("New payment initiated → ledger row recorded: null → PROCESSING with user actor")
    void newPayment_recordsLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();

        service(orderPort(pendingOrder()), noPaymentPort(), savePort,
                gatewayPort("TX-001", "https://pay.example.com"), recorder)
                .execute(ORDER_ID, USER_ID, "payme");

        assertEquals(1, ledgerPort.appended.size());
        var row = ledgerPort.appended.get(0);
        assertNull(row.statusFrom());
        assertEquals(PaymentStatus.PROCESSING, row.statusTo());
        assertEquals(USER_ID, row.actorUserId());
    }

    @Test
    @DisplayName("Idempotency: existing PROCESSING payment → no new save, no ledger row")
    void existingProcessingPayment_idempotent_noLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();
        var existing   = new Payment(5L, ORDER_ID, Money.of(BigDecimal.valueOf(100)), "TJS",
                PaymentStatus.PROCESSING, "payme", "TX-002", "https://pay.example.com",
                Instant.now(), null);
        LoadPaymentPort ppWithExisting = new LoadPaymentPort() {
            @Override public Optional<Payment> findByOrderId(Long id) { return Optional.of(existing); }
            @Override public Optional<Payment> findByProviderTransactionId(String tx) { return Optional.empty(); }
        };

        service(orderPort(pendingOrder()), ppWithExisting, savePort,
                gatewayPort("TX-002", "https://pay.example.com"), recorder)
                .execute(ORDER_ID, USER_ID, "payme");

        assertTrue(savePort.saved.isEmpty());
        assertTrue(ledgerPort.appended.isEmpty());
    }

    @Test
    @DisplayName("Order not PENDING → IllegalStateException, no ledger row")
    void orderNotPending_throws_noLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var paidOrder  = new Order.Builder()
                .id(ORDER_ID).userId(USER_ID).status(OrderStatus.PAID)
                .deliveryType(DeliveryType.HOME)
                .totalAmount(Money.of(BigDecimal.valueOf(100)))
                .createdAt(Instant.now())
                .items(List.of()).build();

        assertThrows(IllegalStateException.class,
                () -> service(orderPort(paidOrder), noPaymentPort(), new CapturingSavePaymentPort(),
                        gatewayPort("TX-003", "url"), recorder).execute(ORDER_ID, USER_ID, "payme"));

        assertTrue(ledgerPort.appended.isEmpty());
    }
}
