package tj.radolfa.application.ports.in.product;

import tj.radolfa.domain.model.Money;

import java.util.List;

/**
 * In-Port: natively create a full product hierarchy without any external system.
 *
 * <p>Called by MANAGER or ADMIN roles through the product-management API.
 */
public interface CreateProductUseCase {

    /**
     * Creates a ProductBase → ListingVariant → SKUs hierarchy.
     *
     * @return the ID of the newly created ProductBase
     */
    Long execute(Command command);

    record Command(
            String name,
            Long   categoryId,
            Long   colorId,
            List<SkuDefinition> skus
    ) {
        public record SkuDefinition(
                String sizeLabel,
                Money  price,
                int    stockQuantity
        ) {}
    }
}
