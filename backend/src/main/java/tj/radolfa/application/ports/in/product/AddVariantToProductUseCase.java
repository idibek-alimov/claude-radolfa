package tj.radolfa.application.ports.in.product;

/**
 * In-Port: add a new (empty) color variant to an existing ProductBase.
 *
 * <p>The new variant is created with no images, no attributes, no SKUs,
 * and {@code isEnabled=false}. Content is filled in afterwards through the edit UI.
 *
 * <p>Called by MANAGER or ADMIN through the product-management API.
 */
public interface AddVariantToProductUseCase {

    Result execute(Command command);

    record Command(Long productBaseId, Long colorId) {}

    record Result(Long variantId, String slug) {}
}
