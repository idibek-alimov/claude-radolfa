package tj.radolfa.application.ports.out;

public interface VerifyPurchasePort {

    /**
     * Returns {@code true} if the user has at least one DELIVERED order
     * containing a SKU from the given listing variant.
     */
    boolean hasPurchasedVariant(Long userId, Long listingVariantId);
}
