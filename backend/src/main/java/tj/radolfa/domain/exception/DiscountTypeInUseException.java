package tj.radolfa.domain.exception;

public class DiscountTypeInUseException extends RuntimeException {

    private final long discountCount;

    public DiscountTypeInUseException(Long typeId, long discountCount) {
        super("Discount type " + typeId + " cannot be deleted — " + discountCount + " discount(s) still reference it");
        this.discountCount = discountCount;
    }

    public long getDiscountCount() {
        return discountCount;
    }
}
