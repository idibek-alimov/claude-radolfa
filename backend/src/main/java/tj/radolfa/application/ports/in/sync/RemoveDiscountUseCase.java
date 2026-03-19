package tj.radolfa.application.ports.in.sync;

/**
 * Handles the on_trash signal from the external source —
 * permanently deletes the Pricing Rule discount.
 */
public interface RemoveDiscountUseCase {

    void execute(String externalRuleId);
}
