package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.UpdateProductCategoryUseCase;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reassigns the category of a ProductBase and re-indexes all its listing
 * variants
 * in Elasticsearch so the change is reflected in search results immediately.
 *
 * <p>
 * MANAGER or ADMIN — enforced at the controller level.
 */
@Service
public class UpdateProductCategoryService implements UpdateProductCategoryUseCase {

        private static final Logger LOG = LoggerFactory.getLogger(UpdateProductCategoryService.class);

        private final LoadProductBasePort loadProductBasePort;
        private final LoadCategoryPort loadCategoryPort;
        private final LoadListingVariantPort loadListingVariantPort;
        private final LoadSkuPort loadSkuPort;
        private final LoadColorPort loadColorPort;
        private final SaveProductHierarchyPort savePort;
        private final ListingIndexPort listingIndexPort;

        public UpdateProductCategoryService(LoadProductBasePort loadProductBasePort,
                        LoadCategoryPort loadCategoryPort,
                        LoadListingVariantPort loadListingVariantPort,
                        LoadSkuPort loadSkuPort,
                        LoadColorPort loadColorPort,
                        SaveProductHierarchyPort savePort,
                        ListingIndexPort listingIndexPort) {
                this.loadProductBasePort = loadProductBasePort;
                this.loadCategoryPort = loadCategoryPort;
                this.loadListingVariantPort = loadListingVariantPort;
                this.loadSkuPort = loadSkuPort;
                this.loadColorPort = loadColorPort;
                this.savePort = savePort;
                this.listingIndexPort = listingIndexPort;
        }

        @Override
        @Transactional
        public void execute(Long productBaseId, Long categoryId) {
                // 1. Resolve category
                CategoryView category = loadCategoryPort.findById(categoryId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Category not found: id=" + categoryId));

                // 2. Load and mutate ProductBase
                ProductBase base = loadProductBasePort.findById(productBaseId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "ProductBase not found: id=" + productBaseId));

                base.updateCategory(category.name(), category.id());
                savePort.saveBase(base);

                LOG.info("[UPDATE-CATEGORY] ProductBase id={} category updated to '{}'",
                                productBaseId, category.name());

                // 3. Re-index all listing variants so the new category is reflected in search
                List<ListingVariant> variants = loadListingVariantPort.findAllByProductBaseId(productBaseId);
                for (ListingVariant variant : variants) {
                        try {
                                reindexVariant(variant, base.getName(), category.name());
                        } catch (Exception ex) {
                                LOG.error("[UPDATE-CATEGORY] ES re-index failed for variant id={}: {}",
                                                variant.getId(), ex.getMessage());
                        }
                }
        }

        private void reindexVariant(ListingVariant variant, String productName, String categoryName) {
                List<Sku> skus = loadSkuPort.findSkusByVariantId(variant.getId());

                Double minPrice = skus.stream()
                                .map(Sku::getPrice)
                                .filter(java.util.Objects::nonNull)
                                .map(Money::amount)
                                .min(BigDecimal::compareTo)
                                .map(BigDecimal::doubleValue)
                                .orElse(null);

                int totalStock = skus.stream()
                                .mapToInt(s -> s.getStockQuantity() != null ? s.getStockQuantity() : 0)
                                .sum();

                String colorHexCode = loadColorPort.findByColorKey(variant.getColorKey())
                                .map(LoadColorPort.ColorView::hexCode)
                                .orElse(null);

                List<String> skuCodes = skus.stream()
                                .map(Sku::getSkuCode)
                                .filter(java.util.Objects::nonNull)
                                .toList();

                listingIndexPort.index(
                                variant.getId(),
                                variant.getProductBaseId(),
                                variant.getSlug(),
                                productName,
                                categoryName,
                                variant.getColorKey(),
                                colorHexCode,
                                variant.getWebDescription(),
                                variant.getImages(),
                                minPrice,
                                totalStock,
                                variant.getLastSyncAt(),
                                variant.getProductCode(),
                                skuCodes);
        }
}
