package tj.radolfa.application.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.VerifyPickupByCodeUseCase;
import tj.radolfa.application.ports.out.LoadDeliveryCodeByValuePort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPickpointCodeLockoutPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveDeliveryCodePort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.SavePickpointCodeLockoutPort;
import tj.radolfa.domain.exception.DeliveryCodeAlreadyUsedException;
import tj.radolfa.domain.exception.DeliveryCodeAttemptsExhaustedException;
import tj.radolfa.domain.exception.DeliveryCodeExpiredException;
import tj.radolfa.domain.exception.DeliveryCodeNotFoundException;
import tj.radolfa.domain.exception.PickpointAccessDeniedException;
import tj.radolfa.domain.exception.PickpointCodeLockoutException;
import tj.radolfa.domain.model.DeliveryCode;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PickpointCodeLockout;

import java.time.Instant;

@Service
public class VerifyPickupByCodeService implements VerifyPickupByCodeUseCase {

    private final LoadDeliveryCodeByValuePort  loadDeliveryCodeByValuePort;
    private final SaveDeliveryCodePort         saveDeliveryCodePort;
    private final LoadOrderPort                loadOrderPort;
    private final SaveOrderPort                saveOrderPort;
    private final LoadUserPort                 loadUserPort;
    private final OrderNotificationService     orderNotificationService;
    private final LoadPickpointCodeLockoutPort loadLockoutPort;
    private final SavePickpointCodeLockoutPort saveLockoutPort;
    private final PickpointCodeFailureRecorder failureRecorder;
    private final int                          maxAttempts;

    public VerifyPickupByCodeService(LoadDeliveryCodeByValuePort loadDeliveryCodeByValuePort,
                                     SaveDeliveryCodePort saveDeliveryCodePort,
                                     LoadOrderPort loadOrderPort,
                                     SaveOrderPort saveOrderPort,
                                     LoadUserPort loadUserPort,
                                     OrderNotificationService orderNotificationService,
                                     LoadPickpointCodeLockoutPort loadLockoutPort,
                                     SavePickpointCodeLockoutPort saveLockoutPort,
                                     PickpointCodeFailureRecorder failureRecorder,
                                     @Value("${radolfa.delivery.code-max-attempts:5}") int maxAttempts) {
        this.loadDeliveryCodeByValuePort = loadDeliveryCodeByValuePort;
        this.saveDeliveryCodePort        = saveDeliveryCodePort;
        this.loadOrderPort               = loadOrderPort;
        this.saveOrderPort               = saveOrderPort;
        this.loadUserPort                = loadUserPort;
        this.orderNotificationService    = orderNotificationService;
        this.loadLockoutPort             = loadLockoutPort;
        this.saveLockoutPort             = saveLockoutPort;
        this.failureRecorder             = failureRecorder;
        this.maxAttempts                 = maxAttempts;
    }

    @Override
    @Transactional
    public void execute(String code, Long staffUserId) {
        var staff = loadUserPort.loadById(staffUserId)
                .orElseThrow(() -> new IllegalStateException("Staff user not found: " + staffUserId));

        Long pickpointId = staff.pickpointId();

        // Per-pickpoint rate-limit pre-check — rejected before touching the code lookup.
        // Being already locked does NOT count as a new failure (prevents lockout extension).
        loadLockoutPort.findByPickpointId(pickpointId)
                .filter(PickpointCodeLockout::isActive)
                .ifPresent(l -> { throw new PickpointCodeLockoutException(l.getLockedUntil()); });

        try {
            DeliveryCode deliveryCode = loadDeliveryCodeByValuePort.loadActiveByCode(code)
                    .orElseThrow(() -> new DeliveryCodeNotFoundException(
                            "No active delivery code found for value: " + code));

            if (deliveryCode.isExpired()) {
                throw new DeliveryCodeExpiredException("Delivery code has expired.");
            }
            if (deliveryCode.isUsed()) {
                throw new DeliveryCodeAlreadyUsedException("Delivery code has already been used.");
            }
            if (deliveryCode.getAttemptCount() >= maxAttempts) {
                throw new DeliveryCodeAttemptsExhaustedException(
                        "Maximum verification attempts reached.");
            }

            Order order = loadOrderPort.loadById(deliveryCode.getOrderId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Order not found for delivery code: " + deliveryCode.getOrderId()));

            if (pickpointId == null || !pickpointId.equals(order.pickpointId())) {
                throw new PickpointAccessDeniedException(
                        "Staff " + staffUserId + " is not assigned to pickpoint " + order.pickpointId());
            }

            if (order.status() != OrderStatus.READY_FOR_PICKUP) {
                throw new IllegalStateException(
                        "Order is not READY_FOR_PICKUP: " + order.status());
            }

            deliveryCode.markUsed();
            saveDeliveryCodePort.save(deliveryCode);

            Order delivered = order.toBuilder()
                    .status(OrderStatus.DELIVERED)
                    .deliveredAt(Instant.now())
                    .build();
            saveOrderPort.save(delivered);
            orderNotificationService.notify(delivered);

            // Reset the per-pickpoint failure counter on success.
            loadLockoutPort.findByPickpointId(pickpointId).ifPresent(lockout -> {
                lockout.resetOnSuccess();
                saveLockoutPort.save(lockout);
            });

        } catch (RuntimeException ex) {
            // Record failure in its own REQUIRES_NEW transaction so it commits
            // independently of this transaction's rollback.
            failureRecorder.recordFailure(pickpointId);
            throw ex;
        }
    }
}
