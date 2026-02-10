package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

public interface SaveProductHierarchyPort {

    ProductBase saveBase(ProductBase base);

    ListingVariant saveVariant(ListingVariant variant, Long productBaseId);

    Sku saveSku(Sku sku, Long listingVariantId);
}
