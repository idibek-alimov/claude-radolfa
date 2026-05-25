package tj.radolfa.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tj.radolfa.application.services.PaymentExpiryService;

@Component
public class PaymentExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryJob.class);

    private final PaymentExpiryService service;

    public PaymentExpiryJob(PaymentExpiryService service) {
        this.service = service;
    }

    @Scheduled(fixedRateString = "${radolfa.payment.sweep-interval-ms:900000}")
    public void run() {
        log.info("[PAYMENT-EXPIRY] Scheduler triggered — starting sweep");
        service.runSweep();
    }
}
