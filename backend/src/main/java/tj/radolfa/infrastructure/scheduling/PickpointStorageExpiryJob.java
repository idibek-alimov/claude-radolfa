package tj.radolfa.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tj.radolfa.application.services.PickpointStorageExpiryService;

@Component
public class PickpointStorageExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PickpointStorageExpiryJob.class);

    private final PickpointStorageExpiryService service;

    public PickpointStorageExpiryJob(PickpointStorageExpiryService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void run() {
        log.info("[PICKPOINT-EXPIRY] Scheduler triggered — starting daily sweep");
        service.runDailySweep();
    }
}
