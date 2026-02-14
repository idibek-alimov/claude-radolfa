package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Sku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles an inbound ERP sync event for a full product hierarchy.
 *
 * <p>Strategy — idempotent upsert across three tiers:
 * <ol>
 *   <li><b>ProductBase</b>: Find-or-create by templateCode. Always update name.</li>
 *   <li><b>ListingVariant</b>: Find-or-create by (baseId, colorKey).
 *       Never overwrite webDescription/images if they already exist.</li>
 *   <li><b>Sku</b>: Find-or-create by erpItemCode. Always overwrite price/stock.</li>
 * </ol>
 */
@Service
public class SyncProductHierarchyService implements SyncProductHierarchyUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyncProductHierarchyService.class);

    private final LoadProductBasePort      loadBasePort;
    private final LoadListingVariantPort   loadVariantPort;
    private final LoadSkuPort              loadSkuPort;
    private final SaveProductHierarchyPort savePort;
    private final ListingIndexPort         listingIndexPort;

    public SyncProductHierarchyService(LoadProductBasePort loadBasePort,
                                       LoadListingVariantPort loadVariantPort,
                                       LoadSkuPort loadSkuPort,
                                       SaveProductHierarchyPort savePort,
                                       ListingIndexPort listingIndexPort) {
        this.loadBasePort      = loadBasePort;
        this.loadVariantPort   = loadVariantPort;
        this.loadSkuPort       = loadSkuPort;
        this.savePort          = savePort;
        this.listingIndexPort  = listingIndexPort;
    }

    @Override
    @Transactional
    public ProductBase execute(HierarchySyncCommand command) {

        LOG.info("[HIERARCHY-SYNC] Processing template={}", command.templateCode());

        // --- Tier 1: ProductBase ---
        ProductBase base = loadBasePort.findByErpTemplateCode(command.templateCode())
                .orElseGet(() -> new ProductBase(null, command.templateCode(), null, null));

        base.updateFromErp(command.templateName(), command.category());
        ProductBase savedBase = savePort.saveBase(base);

        // --- Tier 2 + 3: Variants and SKUs ---
        for (HierarchySyncCommand.VariantCommand vc : command.variants()) {
            final Long baseId = savedBase.getId();
            ListingVariant variant = loadVariantPort
                    .findByProductBaseIdAndColorKey(baseId, vc.colorKey())
                    .orElseGet(() -> new ListingVariant(
                            null,
                            baseId,
                            vc.colorKey(),
                            null,               // slug — will be generated
                            null,               // webDescription — empty skeleton
                            Collections.emptyList(),
                            null                // lastSyncAt — stamped by markSynced
                    ));

            // Generate slug on first creation only (idempotent)
            variant.generateSlug(command.templateCode());
            variant.markSynced();
            ListingVariant savedVariant = savePort.saveVariant(variant, baseId);

            List<Sku> savedSkus = new ArrayList<>();
            for (HierarchySyncCommand.SkuCommand sc : vc.items()) {
                final Long variantId = savedVariant.getId();
                Sku sku = loadSkuPort.findByErpItemCode(sc.erpItemCode())
                        .orElseGet(() -> new Sku(
                                null,
                                variantId,
                                sc.erpItemCode(),
                                sc.sizeLabel(),
                                0,
                                null,
                                null,
                                null
                        ));

                sku.updateSizeLabel(sc.sizeLabel());
                sku.updateFromErp(
                        sc.stockQuantity(),
                        sc.listPrice(),
                        sc.effectivePrice(),
                        sc.saleEndsAt()
                );

                savedSkus.add(savePort.saveSku(sku, variantId));
            }

            // Index into Elasticsearch (fire-and-forget)
            indexVariant(savedVariant, command.templateName(), savedSkus);
        }

        LOG.info("[HIERARCHY-SYNC] Completed template={}, variants={}",
                command.templateCode(), command.variants().size());

        return savedBase;
    }

    private void indexVariant(ListingVariant variant, String productName, List<Sku> skus) {
        Double priceStart = skus.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : s.getPrice())
                .filter(java.util.Objects::nonNull)
                .map(Money::amount)
                .min(java.math.BigDecimal::compareTo)
                .map(java.math.BigDecimal::doubleValue)
                .orElse(null);

        Double priceEnd = skus.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : s.getPrice())
                .filter(java.util.Objects::nonNull)
                .map(Money::amount)
                .max(java.math.BigDecimal::compareTo)
                .map(java.math.BigDecimal::doubleValue)
                .orElse(null);

        int totalStock = skus.stream()
                .mapToInt(s -> s.getStockQuantity() != null ? s.getStockQuantity() : 0)
                .sum();

        listingIndexPort.index(
                variant.getId(),
                variant.getSlug(),
                productName,
                variant.getColorKey(),
                variant.getWebDescription(),
                variant.getImages(),
                priceStart,
                priceEnd,
                totalStock,
                variant.getLastSyncAt()
        );
    }
}
