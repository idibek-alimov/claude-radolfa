package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.CreateOrderUseCase;
import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final SaveOrderPort saveOrderPort;
    private final LoadProductPort loadProductPort;

    public CreateOrderService(SaveOrderPort saveOrderPort, LoadProductPort loadProductPort) {
        this.saveOrderPort = saveOrderPort;
        this.loadProductPort = loadProductPort;
    }

    @Override
    @Transactional
    public Order execute(Long userId, Map<String, Integer> items) {
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String erpId = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = loadProductPort.load(erpId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + erpId));

            // Allow price to be zero or handle null if needed, but for now strict
            BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;

            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(quantity));
            total = total.add(itemTotal);

            orderItems.add(new OrderItem(
                    null, // new item
                    product.getId(),
                    product.getName(),
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
