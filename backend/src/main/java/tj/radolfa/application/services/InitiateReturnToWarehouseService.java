package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.InitiateReturnToWarehouseUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.PickpointAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.UserRole;

import java.time.Instant;

@Service
public class InitiateReturnToWarehouseService implements InitiateReturnToWarehouseUseCase {

    private final LoadOrderPort    loadOrderPort;
    private final SaveOrderPort    saveOrderPort;
    private final LoadUserPort     loadUserPort;
    private final NotificationPort notificationPort;

    public InitiateReturnToWarehouseService(LoadOrderPort loadOrderPort,
                                             SaveOrderPort saveOrderPort,
                                             LoadUserPort loadUserPort,
                                             NotificationPort notificationPort) {
        this.loadOrderPort    = loadOrderPort;
        this.saveOrderPort    = saveOrderPort;
        this.loadUserPort     = loadUserPort;
        this.notificationPort = notificationPort;
    }

    @Override
    @Transactional
    public void execute(Long orderId, Long initiatingUserId) {
        var order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.status() != OrderStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException("Order is not READY_FOR_PICKUP: " + order.status());
        }

        var user = loadUserPort.loadById(initiatingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + initiatingUserId));

        boolean isAdminLike = user.role() == UserRole.ADMIN || user.role() == UserRole.MANAGER;
        if (!isAdminLike) {
            if (user.pickpointId() == null || !user.pickpointId().equals(order.pickpointId())) {
                throw new PickpointAccessDeniedException(
                        "Staff " + initiatingUserId + " is not assigned to pickpoint " + order.pickpointId());
            }
        }

        saveOrderPort.save(order.toBuilder()
                .status(OrderStatus.RETURN_INITIATED)
                .returnInitiatedAt(Instant.now())
                .returnInitiatedByUserId(initiatingUserId)
                .build());

        notificationPort.sendReturnInitiatedNotification(order.userId(), order.id());
    }
}
