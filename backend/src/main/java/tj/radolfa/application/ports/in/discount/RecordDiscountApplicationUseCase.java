package tj.radolfa.application.ports.in.discount;

import java.math.BigDecimal;

public interface RecordDiscountApplicationUseCase {

    void execute(Command command);

    record Command(
            Long discountId,
            Long orderId,
            Long orderLineId,
            String skuItemCode,
            int quantity,
            BigDecimal originalUnitPrice,
            BigDecimal appliedUnitPrice
    ) {}
}
