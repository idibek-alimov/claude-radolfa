package tj.radolfa.domain.exception;

import tj.radolfa.domain.model.ProductStatus;

public class IllegalProductStatusTransitionException extends RuntimeException {

    private final ProductStatus current;
    private final ProductStatus attempted;

    public IllegalProductStatusTransitionException(ProductStatus current, ProductStatus attempted) {
        super("Cannot transition product from " + current + " to " + attempted);
        this.current   = current;
        this.attempted = attempted;
    }

    public ProductStatus getCurrent()   { return current; }
    public ProductStatus getAttempted() { return attempted; }
}
