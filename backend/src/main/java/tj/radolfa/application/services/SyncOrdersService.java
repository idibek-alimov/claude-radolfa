package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.sync.SyncOrdersUseCase;
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
    public SyncResult execute(SyncOrderCommand command) {
        var userOpt = loadUserPort.loadByPhone(command.customerPhone());
        if (userOpt.isEmpty()) {
            String reason = "No user found for phone=" + command.customerPhone()
                    + ", order=" + command.externalOrderId();
            LOG.warn("[ORDER-SYNC] {}", reason);
            return SyncResult.skipped(reason);
        }

        var user = userOpt.get();
        OrderStatus status = parseStatus(command.status());
        Money totalAmount = Money.of(command.totalAmount());

        List<OrderItem> items = command.items().stream()
                .map(item -> {
                    var skuOpt = loadSkuPort.findBySkuCode(item.skuCode());
                    if (skuOpt.isEmpty()) {
                        LOG.warn("[ORDER-SYNC] No SKU found for skuCode={}, order={} — skipping item",
                                item.skuCode(), command.externalOrderId());
                        return null;
                    }
                    return new OrderItem(
                            null,
                            skuOpt.get().getId(),
                            item.skuCode(),
                            item.productName(),
                            item.quantity(),
                            Money.of(item.price()));
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        var existingOpt = loadOrderPort.loadByExternalOrderId(command.externalOrderId());

        if (existingOpt.isPresent()) {
            var existing = existingOpt.get();
            Order updated = new Order(
                    existing.id(),
                    existing.userId(),
                    existing.externalOrderId(),
                    status,
                    totalAmount,
                    items,
                    existing.createdAt());
            saveOrderPort.save(updated);
            LOG.info("[ORDER-SYNC] Updated order externalId={}, status={}", command.externalOrderId(), status);
        } else {
            Order newOrder = new Order(
                    null,
                    user.id(),
                    command.externalOrderId(),
                    status,
                    totalAmount,
                    items,
                    Instant.now());
            saveOrderPort.save(newOrder);
            LOG.info("[ORDER-SYNC] Created order externalId={} for phone={}", command.externalOrderId(), command.customerPhone());
        }

        return SyncResult.synced(command.externalOrderId());
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
