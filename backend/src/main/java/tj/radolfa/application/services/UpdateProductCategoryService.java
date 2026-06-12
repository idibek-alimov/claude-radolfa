package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import tj.radolfa.application.ports.in.product.UpdateProductCategoryUseCase;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;

import java.util.List;

/**
 * Reassigns the category of a ProductBase and re-indexes all its listing
 * variants in Elasticsearch so the change is reflected in search results immediately.
 *
 * <p>MANAGER or ADMIN — enforced at the controller level.
 */
@Service
public class UpdateProductCategoryService implements UpdateProductCategoryUseCase {

        private static final Logger LOG = LoggerFactory.getLogger(UpdateProductCategoryService.class);

        private final LoadProductBasePort         loadProductBasePort;
        private final LoadCategoryPort            loadCategoryPort;
        private final LoadListingVariantPort      loadListingVariantPort;
        private final SaveProductHierarchyPort    savePort;
        private final ApplicationEventPublisher   eventPublisher;
        private final ProductEditGuard            editGuard;
        private final ListingVariantIndexPayload  indexPayload;

        public UpdateProductCategoryService(LoadProductBasePort loadProductBasePort,
                        LoadCategoryPort loadCategoryPort,
                        LoadListingVariantPort loadListingVariantPort,
                        SaveProductHierarchyPort savePort,
                        ApplicationEventPublisher eventPublisher,
                        ProductEditGuard editGuard,
                        ListingVariantIndexPayload indexPayload) {
                this.loadProductBasePort    = loadProductBasePort;
                this.loadCategoryPort       = loadCategoryPort;
                this.loadListingVariantPort = loadListingVariantPort;
                this.savePort               = savePort;
                this.eventPublisher         = eventPublisher;
                this.editGuard              = editGuard;
                this.indexPayload           = indexPayload;
        }

        @Override
        @Transactional
        public void execute(Long productBaseId, Long categoryId) {
                editGuard.resetIfNeeded(productBaseId);

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
                        eventPublisher.publishEvent(indexPayload.build(variant, base));
                }
        }
}
