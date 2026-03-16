package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.SyncOrdersUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadProductVariantPort;
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
    private final LoadProductVariantPort loadVariantPort;

    public SyncOrdersService(LoadOrderPort loadOrderPort,
                             SaveOrderPort saveOrderPort,
                             LoadUserPort loadUserPort,
                             LoadProductVariantPort loadVariantPort) {
        this.loadOrderPort = loadOrderPort;
        this.saveOrderPort = saveOrderPort;
        this.loadUserPort = loadUserPort;
        this.loadVariantPort = loadVariantPort;
    }

    @Override
    @Transactional
    public SyncResult execute(SyncOrderCommand command) {
        var userOpt = loadUserPort.loadByPhone(command.customerPhone());
        if (userOpt.isEmpty()) {
            String reason = "No user found for phone=" + command.customerPhone()
                    + ", order=" + command.erpOrderId();
            LOG.warn("[ORDER-SYNC] {}", reason);
            return SyncResult.skipped(reason);
        }

        var user = userOpt.get();
        OrderStatus status = parseStatus(command.status());
        Money totalAmount = Money.of(command.totalAmount());

        List<OrderItem> items = command.items().stream()
                .map(item -> {
                    var variantOpt = loadVariantPort.findByErpVariantCode(item.erpItemCode());
                    if (variantOpt.isEmpty()) {
                        LOG.warn("[ORDER-SYNC] No variant found for erpItemCode={}, order={} — skipping item",
                                item.erpItemCode(), command.erpOrderId());
                        return null;
                    }
                    return new OrderItem(
                            null,
                            variantOpt.get().getId(),
                            item.erpItemCode(),
                            item.productName(),
                            item.quantity(),
                            Money.of(item.price()));
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        var existingOpt = loadOrderPort.loadByErpOrderId(command.erpOrderId());

        if (existingOpt.isPresent()) {
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

        return SyncResult.synced(command.erpOrderId());
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
