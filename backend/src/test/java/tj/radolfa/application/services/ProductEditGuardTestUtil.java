package tj.radolfa.application.services;

import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ProductEditGuardTestUtil {

    private ProductEditGuardTestUtil() {}

    static ProductEditGuard noOp() {
        LoadProductBasePort noLoad = new LoadProductBasePort() {
            @Override public Optional<ProductBase> findById(Long id) { return Optional.empty(); }
            @Override public Optional<ProductBase> findByExternalRef(String r) { return Optional.empty(); }
            @Override public Map<Long, ProductBase> findProductsByIds(Collection<Long> ids) { return Map.of(); }
        };
        SaveProductHierarchyPort noSave = new SaveProductHierarchyPort() {
            @Override public ProductBase saveBase(ProductBase b) { return b; }
            @Override public ListingVariant saveVariant(ListingVariant v, Long id) { return v; }
            @Override public Sku saveSku(Sku s, Long id) { return s; }
        };
        LoadListingVariantPort noVariant = new LoadListingVariantPort() {
            @Override public Optional<ListingVariant> findVariantById(Long id) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long p, String c) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
            @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return List.of(); }
            @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
        };
        return new ProductEditGuard(noLoad, noSave, skuId -> Optional.empty(), noVariant);
    }
}
