package tj.radolfa.application.ports.in;

/**
 * Handles the {@code on_trash} signal from ERPNext —
 * permanently deletes the Pricing Rule discount.
 */
public interface RemoveDiscountUseCase {

    void execute(String erpPricingRuleId);
}
