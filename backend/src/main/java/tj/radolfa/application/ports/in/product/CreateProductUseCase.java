package tj.radolfa.application.ports.in.product;

import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductAttribute;

import java.util.List;

/**
 * In-Port: natively create a full product hierarchy without any external system.
 *
 * <p>Called by MANAGER or ADMIN roles through the product-management API.
 */
public interface CreateProductUseCase {

    /**
     * Creates a ProductBase → one or more ListingVariants → SKUs hierarchy.
     *
     * @return the ID of the newly created ProductBase
     */
    Long execute(Command command);

    record Command(
            String name,
            Long   categoryId,
            Long   brandId,
            List<VariantDefinition> variants
    ) {
        public record VariantDefinition(
                Long colorId,
                String webDescription,
                List<ProductAttribute> attributes,
                List<String> images,
                List<SkuDefinition> skus,
                boolean isPublished,
                boolean isActive
        ) {}

        public record SkuDefinition(
                String  sizeLabel,
                Money   price,
                int     stockQuantity,
                String  barcode,
                Double  weightKg,
                Integer widthCm,
                Integer heightCm,
                Integer depthCm
        ) {}
    }
}
