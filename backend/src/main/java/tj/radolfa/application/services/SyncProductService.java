package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import tj.radolfa.application.ports.in.SyncProductUseCase;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadColorImagesPort;
import tj.radolfa.application.ports.out.LoadProductTemplatePort;
import tj.radolfa.application.ports.out.LoadProductVariantPort;
import tj.radolfa.application.ports.out.SaveProductPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductTemplate;
import tj.radolfa.domain.model.ProductVariant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles an inbound ERP sync event for a product template and its variants.
 *
 * <p>Strategy — idempotent upsert across two tiers:
 * <ol>
 *   <li><b>ProductTemplate</b>: Find-or-create by templateCode. Update name, category, active.</li>
 *   <li><b>ProductVariant</b>: Find-or-create by erpVariantCode.
 *       Always overwrite price/stock/active. Never overwrite images (enrichment-owned).</li>
 * </ol>
 *
 * <p>Also builds {@code attributesDefinition} on the template by aggregating
 * all variant attribute keys and their distinct values.
 */
@Service
public class SyncProductService implements SyncProductUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyncProductService.class);

    private final LoadProductTemplatePort loadTemplatePort;
    private final LoadProductVariantPort  loadVariantPort;
    private final SaveProductPort         savePort;
    private final ListingIndexPort        listingIndexPort;
    private final LoadColorImagesPort     loadColorImagesPort;

    public SyncProductService(LoadProductTemplatePort loadTemplatePort,
                              LoadProductVariantPort loadVariantPort,
                              SaveProductPort savePort,
                              ListingIndexPort listingIndexPort,
                              LoadColorImagesPort loadColorImagesPort) {
        this.loadTemplatePort   = loadTemplatePort;
        this.loadVariantPort    = loadVariantPort;
        this.savePort           = savePort;
        this.listingIndexPort   = listingIndexPort;
        this.loadColorImagesPort = loadColorImagesPort;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ProductTemplate execute(SyncProductCommand command) {

        LOG.info("[PRODUCT-SYNC] Processing template={}", command.templateCode());

        // --- Tier 1: ProductTemplate ---
        ProductTemplate template = loadTemplatePort.findByErpTemplateCode(command.templateCode())
                .orElseGet(() -> new ProductTemplate(
                        null, command.templateCode(), null, null, null,
                        null, true, false, false));

        template.updateFromErp(command.templateName(), command.category(), command.disabled());

        // Build attributesDefinition from all variant attributes
        Map<String, List<String>> attrsDef = buildAttributesDefinition(command.variants());
        template.updateAttributesDefinition(attrsDef);

        ProductTemplate savedTemplate = savePort.saveTemplate(template);

        // --- Tier 2: ProductVariants ---
        for (SyncProductCommand.VariantCommand vc : command.variants()) {
            ProductVariant variant = loadVariantPort.findByErpVariantCode(vc.erpVariantCode())
                    .orElseGet(() -> new ProductVariant(
                            null, savedTemplate.getId(), vc.erpVariantCode(),
                            vc.attributes(), null, 0, true, null, null));

            variant.updateAttributes(vc.attributes());
            variant.updateFromErp(Money.of(vc.price()), vc.stockQty(), vc.disabled());
            variant.generateSlug(command.templateCode());
            variant.markSynced();

            ProductVariant savedVariant = savePort.saveVariant(variant, savedTemplate.getId());

            indexVariant(savedVariant, command.templateName(), command.category());
        }

        // If template has no variants (standalone item), create a default variant
        if (command.variants().isEmpty()) {
            ProductVariant standalone = loadVariantPort.findByErpVariantCode(command.templateCode())
                    .orElseGet(() -> new ProductVariant(
                            null, savedTemplate.getId(), command.templateCode(),
                            Map.of(), null, 0, true, null, null));

            standalone.updateFromErp(Money.ZERO, 0, command.disabled());
            standalone.generateSlug(command.templateCode());
            standalone.markSynced();

            savePort.saveVariant(standalone, savedTemplate.getId());
        }

        LOG.info("[PRODUCT-SYNC] Completed template={}, variants={}",
                command.templateCode(), command.variants().size());

        return savedTemplate;
    }

    /**
     * Aggregates all variant attributes into a definition map.
     * E.g. if variants have {"Color":"Red","Size":"S"} and {"Color":"Red","Size":"M"},
     * produces {"Color":["Red"],"Size":["M","S"]}.
     */
    private Map<String, List<String>> buildAttributesDefinition(List<SyncProductCommand.VariantCommand> variants) {
        Map<String, Set<String>> collector = new LinkedHashMap<>();
        for (var vc : variants) {
            for (var entry : vc.attributes().entrySet()) {
                collector.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>())
                         .add(entry.getValue());
            }
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        collector.forEach((k, v) -> result.put(k, new ArrayList<>(v)));
        return result;
    }

    private void indexVariant(ProductVariant variant, String productName, String category) {
        Double price = variant.getPrice() != null
                ? variant.getPrice().amount().doubleValue()
                : null;

        java.util.List<String> images = loadColorImagesPort.findImagesByTemplateAndColor(
                variant.getTemplateId(), variant.getColor());

        listingIndexPort.index(
                variant.getId(),
                variant.getSeoSlug(),
                productName,
                category,
                variant.getColor(),
                null,   // colorHexCode — resolved by reindex from colors table
                null,   // description — enrichment field
                images,
                price,
                variant.getStockQty() != null ? variant.getStockQty() : 0,
                false,  // topSelling — enrichment field
                false,  // featured — enrichment field
                variant.getLastSyncAt()
        );
    }
}
