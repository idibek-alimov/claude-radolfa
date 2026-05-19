package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.order.ExpireOrderUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.model.Order;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PaymentExpiryService {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryService.class);

    private final LoadOrderPort      loadOrderPort;
    private final ExpireOrderUseCase expireOrderUseCase;
    private final int                pendingTimeoutMinutes;

    public PaymentExpiryService(
            LoadOrderPort loadOrderPort,
            ExpireOrderUseCase expireOrderUseCase,
            @Value("${radolfa.payment.pending-timeout-minutes:30}") int pendingTimeoutMinutes) {
        this.loadOrderPort         = loadOrderPort;
        this.expireOrderUseCase    = expireOrderUseCase;
        this.pendingTimeoutMinutes = pendingTimeoutMinutes;
    }

    public void runSweep() {
        Instant cutoff = Instant.now().minus(pendingTimeoutMinutes, ChronoUnit.MINUTES);
        List<Order> stale = loadOrderPort.findExpiredPending(cutoff);
        if (stale.isEmpty()) {
            return;
        }
        log.info("[PAYMENT-EXPIRY] Found {} stale PENDING order(s) to expire.", stale.size());
        for (Order order : stale) {
            try {
                expireOrderUseCase.execute(order.id(), "Payment window expired — automatic cleanup");
                log.info("[PAYMENT-EXPIRY] Expired orderId={}", order.id());
            } catch (Exception e) {
                log.error("[PAYMENT-EXPIRY] Failed to expire orderId={}: {}", order.id(), e.getMessage());
            }
        }
    }
}
