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
     * @return a Result record containing the productBaseId, variantId, and slug of the created product
     */
    Result execute(Command command);

    record Result(Long productBaseId, Long variantId, String slug) {}

    record Command(
            String name,
            Long   categoryId,
            Long   colorId,
            String webDescription,
            List<SkuDefinition> skus
    ) {
        public record SkuDefinition(
                String sizeLabel,
                Money  price,
                int    stockQuantity
        ) {}
    }
}
