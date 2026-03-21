package tj.radolfa.application.ports.in.discount;

import tj.radolfa.domain.model.Discount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface UpdateDiscountUseCase {

    record Command(
            Long id,
            Long typeId,
            List<String> itemCodes,
            BigDecimal discountValue,
            Instant validFrom,
            Instant validUpto,
            String title,
            String colorHex
    ) {}

    Discount execute(Command command);
}
