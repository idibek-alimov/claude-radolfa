package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.in.order.RegenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveDeliveryCodePort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryCode;
import tj.radolfa.domain.model.OrderStatus;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class GenerateDeliveryCodeService
        implements GenerateDeliveryCodeUseCase, RegenerateDeliveryCodeUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerateDeliveryCodeService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoadOrderPort                  loadOrderPort;
    private final LoadUserPort                   loadUserPort;
    private final SaveDeliveryCodePort           saveDeliveryCodePort;
    private final DeliveryCodeNotificationService deliveryCodeNotificationService;
    private final int                            codeExpiryHours;

    public GenerateDeliveryCodeService(LoadOrderPort loadOrderPort,
                                       LoadUserPort loadUserPort,
                                       SaveDeliveryCodePort saveDeliveryCodePort,
                                       DeliveryCodeNotificationService deliveryCodeNotificationService,
                                       @Value("${radolfa.delivery.code-expiry-hours:72}") int codeExpiryHours) {
        this.loadOrderPort                   = loadOrderPort;
        this.loadUserPort                    = loadUserPort;
        this.saveDeliveryCodePort            = saveDeliveryCodePort;
        this.deliveryCodeNotificationService = deliveryCodeNotificationService;
        this.codeExpiryHours                 = codeExpiryHours;
    }

    @Override
    @Transactional
    public DeliveryCode execute(Long orderId) {
        var order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.status() != OrderStatus.SHIPPED && order.status() != OrderStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException(
                    "Cannot generate a delivery code for order in status: " + order.status());
        }

        saveDeliveryCodePort.invalidateAllForOrder(orderId);

        String  code      = String.format("%08d", RANDOM.nextInt(100_000_000));
        Instant expiresAt = Instant.now().plus(codeExpiryHours, ChronoUnit.HOURS);
        DeliveryCode newCode = new DeliveryCode(null, orderId, code, expiresAt, null, 0, Instant.now());
        DeliveryCode saved   = saveDeliveryCodePort.save(newCode);

        loadUserPort.loadById(order.userId()).ifPresentOrElse(
                user -> deliveryCodeNotificationService.send(user.id(), orderId, code, expiresAt),
                ()   -> log.warn("User not found for order id={}, delivery code not sent via SMS", orderId)
        );

        return saved;
    }
}
