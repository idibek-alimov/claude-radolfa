package tj.radolfa.application.ports.in;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Upserts a Pricing Rule discount from ERPNext.
 * If a discount with the given {@code erpPricingRuleId} exists it is updated;
 * otherwise a new record is created.
 */
public interface SyncDiscountUseCase {

    void execute(SyncDiscountCommand command);

    record SyncDiscountCommand(
            String erpPricingRuleId,
            String itemCode,
            BigDecimal discountValue,
            Instant validFrom,
            Instant validUpto,
            boolean disabled
    ) {}
}
