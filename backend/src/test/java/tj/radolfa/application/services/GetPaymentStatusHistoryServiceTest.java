package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.LoadPaymentStatusChangePort;
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

class GetPaymentStatusHistoryServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeLoadPaymentPort implements LoadPaymentPort {
        Payment payment;
        FakeLoadPaymentPort(Payment p) { this.payment = p; }
        @Override public Optional<Payment> findByOrderId(Long orderId) {
            return payment != null && payment.orderId().equals(orderId)
                    ? Optional.of(payment) : Optional.empty();
        }
        @Override public Optional<Payment> findByProviderTransactionId(String txId) { return Optional.empty(); }
    }

    static class FakeLoadPaymentStatusChangePort implements LoadPaymentStatusChangePort {
        final List<PaymentStatusChange> rows = new ArrayList<>();
        Pageable lastPageable;

        @Override
        public Page<PaymentStatusChange> findByPaymentId(Long paymentId, Pageable pageable) {
            this.lastPageable = pageable;
            List<PaymentStatusChange> matching = rows.stream()
                    .filter(r -> r.paymentId().equals(paymentId))
                    .toList();
            int start = (int) pageable.getOffset();
            int end   = Math.min(start + pageable.getPageSize(), matching.size());
            List<PaymentStatusChange> slice = start >= matching.size()
                    ? List.of() : matching.subList(start, end);
            return new PageImpl<>(slice, pageable, matching.size());
        }
    }

    static Payment payment(Long id, Long orderId) {
        return new Payment(id, orderId, Money.of(BigDecimal.valueOf(100)), "TJS",
                PaymentStatus.PROCESSING, "payme", "TX-001", null, Instant.now(), null);
    }

    static PaymentStatusChange row(Long paymentId, PaymentStatus from, PaymentStatus to) {
        return new PaymentStatusChange(null, paymentId, from, to, null, null, Instant.now());
    }

    static GetPaymentStatusHistoryService service(LoadPaymentPort pp, LoadPaymentStatusChangePort cp) {
        return new GetPaymentStatusHistoryService(pp, cp);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns rows for the payment associated with the given orderId")
    void returnsRowsForOrder() {
        var payPort  = new FakeLoadPaymentPort(payment(7L, 1L));
        var histPort = new FakeLoadPaymentStatusChangePort();
        histPort.rows.add(row(7L, null,                   PaymentStatus.PROCESSING));
        histPort.rows.add(row(7L, PaymentStatus.PROCESSING, PaymentStatus.COMPLETED));

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<PaymentStatusChange> result = service(payPort, histPort).execute(1L, pageable);

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(r -> r.paymentId().equals(7L)));
    }

    @Test
    @DisplayName("COD order: no payment row → empty page returned")
    void codOrder_noPayment_returnsEmptyPage() {
        var payPort  = new FakeLoadPaymentPort(null); // no payment for this order
        var histPort = new FakeLoadPaymentStatusChangePort();

        Pageable pageable = PageRequest.of(0, 20);
        Page<PaymentStatusChange> result = service(payPort, histPort).execute(99L, pageable);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("Delegates Pageable unchanged to the load-change port")
    void delegatesPageableToPort() {
        var payPort  = new FakeLoadPaymentPort(payment(7L, 1L));
        var histPort = new FakeLoadPaymentStatusChangePort();
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "occurredAt"));

        service(payPort, histPort).execute(1L, pageable);

        assertSame(pageable, histPort.lastPageable);
    }

    @Test
    @DisplayName("Pagination: page 1 of size 1 returns the second row")
    void pagination_secondPage() {
        var payPort  = new FakeLoadPaymentPort(payment(7L, 1L));
        var histPort = new FakeLoadPaymentStatusChangePort();
        histPort.rows.add(row(7L, null,                    PaymentStatus.PROCESSING));
        histPort.rows.add(row(7L, PaymentStatus.PROCESSING, PaymentStatus.COMPLETED));

        Pageable pageable = PageRequest.of(1, 1);
        Page<PaymentStatusChange> result = service(payPort, histPort).execute(1L, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(PaymentStatus.COMPLETED, result.getContent().get(0).statusTo());
    }
}
