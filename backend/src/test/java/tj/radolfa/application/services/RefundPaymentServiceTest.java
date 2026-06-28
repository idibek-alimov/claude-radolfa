package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.loyalty.RevokeAwardedPointsUseCase;
import tj.radolfa.application.ports.in.order.CancelOrderUseCase;
import tj.radolfa.application.ports.out.PaymentPort;
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

import static org.junit.jupiter.api.Assertions.*;

class RefundPaymentServiceTest {

    static final Long ORDER_ID  = 1L;
    static final Long PAY_ID    = 5L;
    static final Long ADMIN_ID  = 99L;

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeSavePaymentStatusChangePort implements SavePaymentStatusChangePort {
        final List<PaymentStatusChange> appended = new ArrayList<>();
        @Override public PaymentStatusChange append(PaymentStatusChange c) { appended.add(c); return c; }
    }

    static PaymentStatusChangeRecorder noopRecorder() {
        return new PaymentStatusChangeRecorder(new FakeSavePaymentStatusChangePort());
    }

    static Payment completedPayment() {
        return new Payment(PAY_ID, ORDER_ID, Money.of(BigDecimal.valueOf(200)), "TJS",
                PaymentStatus.COMPLETED, "payme", "TX-001", null, Instant.now(), Instant.now());
    }

    static LoadPaymentPort paymentPort(Payment p) {
        return new LoadPaymentPort() {
            @Override public Optional<Payment> findByOrderId(Long id) {
                return id.equals(ORDER_ID) ? Optional.of(p) : Optional.empty();
            }
            @Override public Optional<Payment> findByProviderTransactionId(String tx) { return Optional.empty(); }
        };
    }

    static LoadOrderPort orderPort() {
        Order order = new Order.Builder()
                .id(ORDER_ID).userId(20L).status(OrderStatus.DELIVERED)
                .deliveryType(DeliveryType.HOME)
                .totalAmount(Money.of(BigDecimal.valueOf(200)))
                .createdAt(Instant.now())
                .items(List.of())
                .build();
        return new LoadOrderPort() {
            @Override public Optional<Order> loadById(Long id) { return Optional.of(order); }
            @Override public List<Order> loadByUserId(Long u) { return List.of(); }
            @Override public Optional<Order> loadByExternalOrderId(String e) { return Optional.empty(); }
            @Override public List<Order> loadRecentPaidByUserId(Long u, int l) { return List.of(); }
        };
    }

    static class CapturingSavePaymentPort implements SavePaymentPort {
        final List<Payment> saved = new ArrayList<>();
        @Override public Payment save(Payment p) { saved.add(p); return p; }
    }

    static PaymentPort successfulGateway() {
        return new PaymentPort() {
            @Override public PaymentIntent initiate(Money a, String c, String o, String u) {
                return new PaymentIntent("TX", "url", null);
            }
            @Override public RefundResult refund(String tx, Money amount) {
                return new RefundResult("REFUND-1", true, null);
            }
        };
    }

    static PaymentPort failingGateway() {
        return new PaymentPort() {
            @Override public PaymentIntent initiate(Money a, String c, String o, String u) {
                return new PaymentIntent("TX", "url", null);
            }
            @Override public RefundResult refund(String tx, Money amount) {
                return new RefundResult(null, false, "gateway error");
            }
        };
    }

    static RefundPaymentService service(LoadPaymentPort pp, SavePaymentPort sp,
                                         PaymentPort gp, PaymentStatusChangeRecorder recorder) {
        CancelOrderUseCase cancelNoop = (orderId, actorUserId, reason) -> {};
        RevokeAwardedPointsUseCase revokeNoop = (userId, points) -> {};
        return new RefundPaymentService(pp, sp, gp, orderPort(), cancelNoop, revokeNoop, recorder);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("COMPLETED → REFUNDED records one ledger row with adminUserId")
    void refund_recordsLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();

        service(paymentPort(completedPayment()), savePort, successfulGateway(), recorder)
                .execute(ORDER_ID, ADMIN_ID);

        assertEquals(1, savePort.saved.size());
        assertEquals(PaymentStatus.REFUNDED, savePort.saved.get(0).status());

        assertEquals(1, ledgerPort.appended.size());
        var row = ledgerPort.appended.get(0);
        assertEquals(PaymentStatus.COMPLETED, row.statusFrom());
        assertEquals(PaymentStatus.REFUNDED,  row.statusTo());
        assertEquals(ADMIN_ID, row.actorUserId());
    }

    @Test
    @DisplayName("Not COMPLETED → IllegalStateException, no save, no ledger row")
    void notCompleted_throws_noLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();
        var pending = new Payment(PAY_ID, ORDER_ID, Money.of(BigDecimal.valueOf(200)), "TJS",
                PaymentStatus.PROCESSING, "payme", "TX-001", null, Instant.now(), null);

        assertThrows(IllegalStateException.class,
                () -> service(paymentPort(pending), savePort, successfulGateway(), recorder)
                        .execute(ORDER_ID, ADMIN_ID));

        assertTrue(savePort.saved.isEmpty());
        assertTrue(ledgerPort.appended.isEmpty());
    }

    @Test
    @DisplayName("Gateway failure → IllegalStateException, no save, no ledger row")
    void gatewayFailure_throws_noLedgerRow() {
        var ledgerPort = new FakeSavePaymentStatusChangePort();
        var recorder   = new PaymentStatusChangeRecorder(ledgerPort);
        var savePort   = new CapturingSavePaymentPort();

        assertThrows(IllegalStateException.class,
                () -> service(paymentPort(completedPayment()), savePort, failingGateway(), recorder)
                        .execute(ORDER_ID, ADMIN_ID));

        assertTrue(savePort.saved.isEmpty());
        assertTrue(ledgerPort.appended.isEmpty());
    }
}
