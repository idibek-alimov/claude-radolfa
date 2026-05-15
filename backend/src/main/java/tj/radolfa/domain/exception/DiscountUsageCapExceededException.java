package tj.radolfa.domain.exception;

public class DiscountUsageCapExceededException extends RuntimeException {

    public enum Scope { TOTAL, PER_CUSTOMER }

    private final Long discountId;
    private final Scope scope;

    public DiscountUsageCapExceededException(Long discountId, Scope scope) {
        super("Discount usage cap exceeded for discount id=" + discountId + " (scope=" + scope + ")");
        this.discountId = discountId;
        this.scope = scope;
    }

    public Long getDiscountId() { return discountId; }
    public Scope getScope()     { return scope; }
}
