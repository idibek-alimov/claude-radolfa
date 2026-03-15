package tj.radolfa.application.ports.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Upserts a Pricing Rule discount from ERPNext.
 * If a discount with the given {@code erpPricingRuleId} exists it is updated;
 * otherwise a new record is created.
 *
 * <p>One pricing rule may cover multiple SKU item codes.
 */
public interface SyncDiscountUseCase {

    void execute(SyncDiscountCommand command);

    record SyncDiscountCommand(
            String erpPricingRuleId,
            List<String> itemCodes,
            BigDecimal discountValue,
            Instant validFrom,
            Instant validUpto,
            boolean disabled,
            String title,
            String colorHex
    ) {}
}
