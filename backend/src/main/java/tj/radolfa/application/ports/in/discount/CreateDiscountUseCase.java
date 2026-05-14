package tj.radolfa.application.ports.in.discount;

import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountTarget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface CreateDiscountUseCase {

    record Command(
            Long typeId,
            List<DiscountTarget> targets,
            AmountType amountType,
            BigDecimal amountValue,
            Instant validFrom,
            Instant validUpto,
            String title,
            String colorHex,
            BigDecimal minBasketAmount,
            Integer usageCapTotal,
            Integer usageCapPerCustomer,
            String couponCode
    ) {}

    Discount execute(Command command);
}
