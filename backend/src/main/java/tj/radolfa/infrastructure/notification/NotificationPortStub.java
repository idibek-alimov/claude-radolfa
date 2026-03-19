package tj.radolfa.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.domain.model.OrderStatus;

/**
 * Dev/test stub for {@link NotificationPort}.
 *
 * <p>Logs all calls to the console. Replace with a real SMS/push adapter in a
 * later phase.
 */
@Component
@Profile("dev | test")
public class NotificationPortStub implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationPortStub.class);

    @Override
    public void sendOrderConfirmation(Long userId, Long orderId) {
        log.info("[NOTIFICATION STUB] Order confirmation → userId={} orderId={}", userId, orderId);
    }

    @Override
    public void sendOrderStatusUpdate(Long userId, Long orderId, OrderStatus newStatus) {
        log.info("[NOTIFICATION STUB] Order status update → userId={} orderId={} status={}",
                userId, orderId, newStatus);
    }
}
