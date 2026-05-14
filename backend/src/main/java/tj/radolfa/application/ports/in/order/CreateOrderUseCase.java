package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.Money;

import java.util.List;

/**
 * In-Port: create an order from explicit line items.
 *
 * <p>Lower-level than {@link CheckoutUseCase}. Used by ADMIN for manual
 * order creation and by the import path for legacy order migration.
 * Does NOT validate cart state or decrement stock automatically.
 */
public interface CreateOrderUseCase {

    /**
     * @return the ID of the newly created order
     */
    Long execute(Command command);

    record Command(
            Long         userId,
            List<LineItem> items,
            String       externalOrderId,  // nullable — set only for imported orders
            String       notes
    ) {
        public record LineItem(Long skuId, int quantity, Money unitPrice) {}
    }
}
