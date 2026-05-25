package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ApproveRefundUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.ProcessRefundPort;
import tj.radolfa.application.ports.out.SaveCustomerReturnPort;
import tj.radolfa.domain.exception.RefundFailedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ApproveRefundService implements ApproveRefundUseCase {

    private final LoadCustomerReturnPort loadCustomerReturnPort;
    private final SaveCustomerReturnPort saveCustomerReturnPort;
    private final LoadOrderPort          loadOrderPort;
    private final LoadUserPort           loadUserPort;
    private final LoadPaymentPort        loadPaymentPort;
    private final ProcessRefundPort      processRefundPort;
    private final NotificationPort       notificationPort;

    public ApproveRefundService(LoadCustomerReturnPort loadCustomerReturnPort,
                                SaveCustomerReturnPort saveCustomerReturnPort,
                                LoadOrderPort loadOrderPort,
                                LoadUserPort loadUserPort,
                                LoadPaymentPort loadPaymentPort,
                                ProcessRefundPort processRefundPort,
                                NotificationPort notificationPort) {
        this.loadCustomerReturnPort = loadCustomerReturnPort;
        this.saveCustomerReturnPort = saveCustomerReturnPort;
        this.loadOrderPort          = loadOrderPort;
        this.loadUserPort           = loadUserPort;
        this.loadPaymentPort        = loadPaymentPort;
        this.processRefundPort      = processRefundPort;
        this.notificationPort       = notificationPort;
    }

    @Override
    @Transactional
    public void execute(Long returnId, Long adminUserId) {
        var customerReturn = loadCustomerReturnPort.loadById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CustomerReturn with id " + returnId + " not found"));

        if (customerReturn.getStatus() != CustomerReturnStatus.SENT_TO_WAREHOUSE) {
            throw new IllegalStateException(
                    "Refund can only be approved for returns in SENT_TO_WAREHOUSE state, but was: "
                    + customerReturn.getStatus());
        }

        Order order = loadOrderPort.loadById(customerReturn.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found for return: " + customerReturn.getOrderId()));

        Map<Long, OrderItem> orderItemMap = order.items().stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        Money totalRefundAmount = customerReturn.getItems().stream()
                .map(ri -> {
                    OrderItem oi = orderItemMap.get(ri.orderItemId());
                    return oi != null ? oi.getPrice().multiply(ri.quantity()) : Money.ZERO;
                })
                .reduce(Money.ZERO, Money::add);

        var payment = loadPaymentPort.findByOrderId(order.id())
                .orElseThrow(() -> new IllegalStateException(
                        "No payment found for order " + order.id()));

        ProcessRefundPort.RefundResult result = processRefundPort.process(
                order.id(), payment.providerTransactionId(), totalRefundAmount);

        if (!result.success()) {
            throw new RefundFailedException(
                    result.failureReason() != null ? result.failureReason() : "Gateway returned failure");
        }

        customerReturn.markRefundApproved(adminUserId, result.gatewayRefundId());
        saveCustomerReturnPort.save(customerReturn);

        customerReturn.markRefunded();
        saveCustomerReturnPort.save(customerReturn);

        notificationPort.sendRefundApprovedNotification(order.userId(), order.id(), totalRefundAmount);
    }
}
