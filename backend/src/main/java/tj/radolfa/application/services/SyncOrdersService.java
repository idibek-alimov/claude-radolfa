package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.SyncOrdersUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;
import java.util.List;

@Service
public class SyncOrdersService implements SyncOrdersUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyncOrdersService.class);

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final LoadUserPort loadUserPort;
    private final LoadSkuPort loadSkuPort;

    public SyncOrdersService(LoadOrderPort loadOrderPort,
                             SaveOrderPort saveOrderPort,
                             LoadUserPort loadUserPort,
                             LoadSkuPort loadSkuPort) {
        this.loadOrderPort = loadOrderPort;
        this.saveOrderPort = saveOrderPort;
        this.loadUserPort = loadUserPort;
        this.loadSkuPort = loadSkuPort;
    }

    @Override
    @Transactional
    public void execute(SyncOrderCommand command) {
        var userOpt = loadUserPort.loadByPhone(command.customerPhone());
        if (userOpt.isEmpty()) {
            LOG.warn("[ORDER-SYNC] No user found for phone={}. Skipping order={}",
                    command.customerPhone(), command.erpOrderId());
            return;
        }

        var user = userOpt.get();
        OrderStatus status = parseStatus(command.status());
        Money totalAmount = Money.of(command.totalAmount());

        List<OrderItem> items = command.items().stream()
                .map(item -> {
                    Long skuId = loadSkuPort.findByErpItemCode(item.erpItemCode())
                            .map(sku -> sku.getId())
                            .orElseGet(() -> {
                                LOG.warn("[ORDER-SYNC] No SKU found for erpItemCode={}, order={}",
                                        item.erpItemCode(), command.erpOrderId());
                                return null;
                            });
                    return new OrderItem(
                            null,
                            skuId,
                            item.erpItemCode(),
                            item.productName(),
                            item.quantity(),
                            Money.of(item.price()));
                })
                .toList();

        var existingOpt = loadOrderPort.loadByErpOrderId(command.erpOrderId());

        if (existingOpt.isPresent()) {
            // Update existing order (status, total, items)
            var existing = existingOpt.get();
            Order updated = new Order(
                    existing.id(),
                    existing.userId(),
                    existing.erpOrderId(),
                    status,
                    totalAmount,
                    items,
                    existing.createdAt());
            saveOrderPort.save(updated);
            LOG.info("[ORDER-SYNC] Updated order erpId={}, status={}", command.erpOrderId(), status);
        } else {
            // Create new order
            Order newOrder = new Order(
                    null,
                    user.id(),
                    command.erpOrderId(),
                    status,
                    totalAmount,
                    items,
                    Instant.now());
            saveOrderPort.save(newOrder);
            LOG.info("[ORDER-SYNC] Created order erpId={} for phone={}", command.erpOrderId(), command.customerPhone());
        }
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOG.warn("[ORDER-SYNC] Unknown status '{}', defaulting to PENDING", status);
            return OrderStatus.PENDING;
        }
    }
}
