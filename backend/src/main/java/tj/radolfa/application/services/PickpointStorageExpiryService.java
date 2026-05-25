package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.out.LoadExpiringPickpointOrdersPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.domain.model.Order;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Orchestrates the nightly pickpoint storage warning sweep.
 *
 * <p>Each call to {@link #runDailySweep()} sends pre-expiry SMS warnings to customers
 * whose orders have been at the pickup point for {@code storageDays - warningDays} days.
 * Overdue packages are flagged visually via {@code PickpointOrderDto.overdue} — no automatic
 * cancellation occurs. Staff or admin must manually initiate the return process.
 *
 * <p>Failures in individual iterations are logged at WARN and do not abort the batch.
 */
@Service
public class PickpointStorageExpiryService {

    private static final Logger log = LoggerFactory.getLogger(PickpointStorageExpiryService.class);

    private final LoadExpiringPickpointOrdersPort loadExpiringPickpointOrdersPort;
    private final NotificationPort                notificationPort;
    private final int                             storageDays;
    private final int                             warningDays;

    public PickpointStorageExpiryService(
            LoadExpiringPickpointOrdersPort loadExpiringPickpointOrdersPort,
            NotificationPort notificationPort,
            @Value("${radolfa.delivery.pickpoint-storage-days:7}") int storageDays,
            @Value("${radolfa.delivery.pickpoint-expiry-warning-days:2}") int warningDays) {
        this.loadExpiringPickpointOrdersPort = loadExpiringPickpointOrdersPort;
        this.notificationPort                = notificationPort;
        this.storageDays                     = storageDays;
        this.warningDays                     = warningDays;
    }

    public void runDailySweep() {
        Instant now    = Instant.now();
        int     warned = 0;

        // ── Warning batch ─────────────────────────────────────────────────────
        // Window: orders that became ready exactly (storageDays - warningDays) days ago (±1 day).
        Instant warningWindowEnd   = now.minus(storageDays - warningDays,     ChronoUnit.DAYS);
        Instant warningWindowStart = now.minus(storageDays - warningDays + 1, ChronoUnit.DAYS);
        List<Order> warning = loadExpiringPickpointOrdersPort
                .findReadyForPickupInWindow(warningWindowStart, warningWindowEnd);
        for (Order order : warning) {
            try {
                notificationPort.sendPickpointExpiryWarning(order.userId(), order.id(), warningDays);
                warned++;
            } catch (Exception ex) {
                log.warn("[PICKPOINT-EXPIRY] Failed to warn for orderId={}: {}", order.id(), ex.getMessage());
            }
        }

        log.info("[PICKPOINT-EXPIRY] Daily sweep complete — warned={}", warned);
    }
}
