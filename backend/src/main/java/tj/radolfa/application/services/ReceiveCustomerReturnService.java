package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ReceiveCustomerReturnUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveCustomerReturnPort;
import tj.radolfa.domain.exception.OrderNotAtPickpointException;
import tj.radolfa.domain.exception.OrderNotDeliveredException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.exception.ReturnAlreadyExistsException;
import tj.radolfa.domain.exception.ReturnItemQuantityExceededException;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnItem;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.Resellability;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReceiveCustomerReturnService implements ReceiveCustomerReturnUseCase {

    private final LoadOrderPort           loadOrderPort;
    private final LoadUserPort            loadUserPort;
    private final LoadCustomerReturnPort  loadCustomerReturnPort;
    private final SaveCustomerReturnPort  saveCustomerReturnPort;
    private final NotificationPort        notificationPort;

    public ReceiveCustomerReturnService(LoadOrderPort loadOrderPort,
                                        LoadUserPort loadUserPort,
                                        LoadCustomerReturnPort loadCustomerReturnPort,
                                        SaveCustomerReturnPort saveCustomerReturnPort,
                                        NotificationPort notificationPort) {
        this.loadOrderPort          = loadOrderPort;
        this.loadUserPort           = loadUserPort;
        this.loadCustomerReturnPort = loadCustomerReturnPort;
        this.saveCustomerReturnPort = saveCustomerReturnPort;
        this.notificationPort       = notificationPort;
    }

    @Override
    @Transactional
    public CustomerReturn execute(Command command) {
        var order = loadOrderPort.loadById(command.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + command.orderId()));

        if (order.status() != OrderStatus.DELIVERED) {
            throw new OrderNotDeliveredException(
                    "Order " + order.id() + " is not DELIVERED: " + order.status());
        }

        var staff = loadUserPort.loadById(command.staffUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + command.staffUserId()));

        if (order.deliveryType() != DeliveryType.PICKPOINT
                || staff.pickpointId() == null
                || !staff.pickpointId().equals(order.pickpointId())) {
            throw new OrderNotAtPickpointException(
                    "Order " + order.id() + " is not a PICKPOINT order at this staff's pickpoint");
        }

        List<CustomerReturn> existingReturns = loadCustomerReturnPort.loadAllByOrderId(command.orderId());

        boolean hasOpenReturn = existingReturns.stream()
                .anyMatch(r -> r.getStatus() == CustomerReturnStatus.RECEIVED);
        if (hasOpenReturn) {
            throw new ReturnAlreadyExistsException(
                    "An open return already exists for order " + command.orderId());
        }

        // Build map of how many units have already been returned per orderItemId (across all returns)
        Map<Long, Integer> alreadyReturnedQty = new HashMap<>();
        for (CustomerReturn r : existingReturns) {
            for (CustomerReturnItem item : r.getItems()) {
                alreadyReturnedQty.merge(item.orderItemId(), item.quantity(), Integer::sum);
            }
        }

        // Build a quick lookup map from the order's items
        Map<Long, OrderItem> orderItemMap = new HashMap<>();
        for (OrderItem oi : order.items()) {
            orderItemMap.put(oi.getId(), oi);
        }

        // Validate item-level quantities and build return items
        List<CustomerReturnItem> returnItems = command.items().stream().map(cmd -> {
            OrderItem orderItem = orderItemMap.get(cmd.orderItemId());
            if (orderItem == null) {
                throw new ResourceNotFoundException("OrderItem not found: " + cmd.orderItemId());
            }
            int already = alreadyReturnedQty.getOrDefault(cmd.orderItemId(), 0);
            if (already + cmd.quantity() > orderItem.getQuantity()) {
                throw new ReturnItemQuantityExceededException(
                        "Return quantity " + cmd.quantity() + " for item " + cmd.orderItemId()
                        + " exceeds remaining returnable quantity "
                        + (orderItem.getQuantity() - already));
            }
            return new CustomerReturnItem(null, null, cmd.orderItemId(), cmd.quantity(),
                    cmd.reason(), cmd.notes(), Resellability.PENDING_REVIEW);
        }).toList();

        CustomerReturn customerReturn = new CustomerReturn(
                null,
                order.id(),
                order.pickpointId(),
                command.staffUserId(),
                Instant.now(),
                command.notes(),
                returnItems,
                CustomerReturnStatus.RECEIVED,
                null, null, null, null, null, null);

        CustomerReturn saved = saveCustomerReturnPort.save(customerReturn);
        notificationPort.sendCustomerReturnReceivedNotification(order.userId(), saved.getOrderId());
        return saved;
    }
}
