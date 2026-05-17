package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ConfirmReturnedToWarehouseUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.PickpointAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;

@Service
public class ConfirmReturnedToWarehouseService implements ConfirmReturnedToWarehouseUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final LoadUserPort  loadUserPort;

    public ConfirmReturnedToWarehouseService(LoadOrderPort loadOrderPort,
                                              SaveOrderPort saveOrderPort,
                                              LoadUserPort loadUserPort) {
        this.loadOrderPort = loadOrderPort;
        this.saveOrderPort = saveOrderPort;
        this.loadUserPort  = loadUserPort;
    }

    @Override
    @Transactional
    public void execute(Long orderId, Long staffUserId) {
        var order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.status() != OrderStatus.RETURN_INITIATED) {
            throw new IllegalStateException("Order is not RETURN_INITIATED: " + order.status());
        }

        var staff = loadUserPort.loadById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + staffUserId));

        if (staff.pickpointId() == null || !staff.pickpointId().equals(order.pickpointId())) {
            throw new PickpointAccessDeniedException(
                    "Staff " + staffUserId + " is not assigned to pickpoint " + order.pickpointId());
        }

        saveOrderPort.save(order.toBuilder()
                .status(OrderStatus.RETURNED_TO_WAREHOUSE)
                .returnedToWarehouseAt(Instant.now())
                .build());
    }
}
