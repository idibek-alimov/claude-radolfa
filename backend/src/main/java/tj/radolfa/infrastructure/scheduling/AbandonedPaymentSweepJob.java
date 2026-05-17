package tj.radolfa.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.in.order.ExpireOrderUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.model.Order;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class AbandonedPaymentSweepJob {

    private static final Logger log = LoggerFactory.getLogger(AbandonedPaymentSweepJob.class);

    private final LoadOrderPort    loadOrderPort;
    private final ExpireOrderUseCase expireOrderUseCase;

    @Value("${radolfa.payment.pending-timeout-minutes:30}")
    private int pendingTimeoutMinutes;

    public AbandonedPaymentSweepJob(LoadOrderPort loadOrderPort,
                                    ExpireOrderUseCase expireOrderUseCase) {
        this.loadOrderPort     = loadOrderPort;
        this.expireOrderUseCase = expireOrderUseCase;
    }

    @Scheduled(fixedDelayString = "${radolfa.payment.sweep-interval-ms:300000}")
    public void run() {
        Instant cutoff = Instant.now().minus(pendingTimeoutMinutes, ChronoUnit.MINUTES);
        List<Order> expired = loadOrderPort.findExpiredPending(cutoff);

        if (expired.isEmpty()) return;

        log.info("[PAYMENT-SWEEP] Found {} expired PENDING order(s) — cancelling", expired.size());
        int cancelled = 0;
        for (Order order : expired) {
            try {
                expireOrderUseCase.execute(order.id(), "Payment window expired");
                cancelled++;
            } catch (Exception e) {
                log.warn("[PAYMENT-SWEEP] Failed to expire orderId={}: {}", order.id(), e.getMessage());
            }
        }
        log.info("[PAYMENT-SWEEP] Sweep complete — cancelled={} failed={}",
                cancelled, expired.size() - cancelled);
    }
}
