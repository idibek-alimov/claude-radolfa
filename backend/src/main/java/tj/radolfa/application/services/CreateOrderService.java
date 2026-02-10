package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.CreateOrderUseCase;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final SaveOrderPort saveOrderPort;
    private final tj.radolfa.application.ports.out.LoadSkuPort loadSkuPort;

    public CreateOrderService(SaveOrderPort saveOrderPort, tj.radolfa.application.ports.out.LoadSkuPort loadSkuPort) {
        this.saveOrderPort = saveOrderPort;
        this.loadSkuPort = loadSkuPort;
    }

    @Override
    @Transactional
    public Order execute(Long userId, Map<String, Integer> items) {
        List<OrderItem> orderItems = new ArrayList<>();
        Money total = Money.ZERO;

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String skuCode = entry.getKey();
            int quantity = entry.getValue();

            tj.radolfa.domain.model.Sku sku = loadSkuPort.findByErpItemCode(skuCode)
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + skuCode));

            // Use Sale Price if available and active
            Money price = sku.getSalePrice() != null ? sku.getSalePrice() : sku.getPrice();
            if (price == null)
                price = Money.ZERO;

            Money itemTotal = price.multiply(quantity);
            total = total.add(itemTotal);

            orderItems.add(new OrderItem(
                    null,
                    sku.getId(),
                    sku.getSizeLabel(), // Or product name + size? Keeping simple for now
                    quantity,
                    price));
        }

        Order order = new Order(
                null,
                userId,
                OrderStatus.PENDING,
                total,
                orderItems,
                Instant.now());

        return saveOrderPort.save(order);
    }
}
