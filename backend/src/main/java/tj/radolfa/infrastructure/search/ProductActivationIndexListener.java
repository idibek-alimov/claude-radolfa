package tj.radolfa.infrastructure.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tj.radolfa.application.event.ProductActivatedEvent;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.services.ListingVariantIndexPayload;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;

import java.util.List;

/**
 * When a product flips to ACTIVE via the first putaway, pushes all its listing
 * variants into Elasticsearch so they become immediately searchable.
 *
 * <p>Uses {@link ListingIndexPort#index} directly (not via a republished event)
 * because this listener fires AFTER_COMMIT — a nested AFTER_COMMIT event would
 * never be picked up by another listener.
 */
@Slf4j
@Component
public class ProductActivationIndexListener {

    private final LoadProductBasePort        loadProductBasePort;
    private final LoadListingVariantPort     loadListingVariantPort;
    private final ListingVariantIndexPayload indexPayload;
    private final ListingIndexPort           listingIndexPort;

    public ProductActivationIndexListener(LoadProductBasePort loadProductBasePort,
                                          LoadListingVariantPort loadListingVariantPort,
                                          ListingVariantIndexPayload indexPayload,
                                          ListingIndexPort listingIndexPort) {
        this.loadProductBasePort    = loadProductBasePort;
        this.loadListingVariantPort = loadListingVariantPort;
        this.indexPayload           = indexPayload;
        this.listingIndexPort       = listingIndexPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductActivated(ProductActivatedEvent event) {
        try {
            ProductBase base = loadProductBasePort.findById(event.productBaseId())
                    .orElse(null);
            if (base == null) {
                log.warn("[ES] ProductActivationIndexListener: base not found id={}", event.productBaseId());
                return;
            }

            List<ListingVariant> variants =
                    loadListingVariantPort.findAllByProductBaseId(event.productBaseId());

            for (ListingVariant variant : variants) {
                var ev = indexPayload.build(variant, base);
                listingIndexPort.index(
                        ev.variantId(), ev.productBaseId(), ev.slug(),
                        ev.name(), ev.category(), ev.colorKey(), ev.colorHexCode(),
                        ev.description(), ev.images(), ev.price(), ev.totalStock(),
                        ev.lastSyncAt(), ev.productCode(), ev.skuCodes(), ev.status(),
                        ev.categoryId(), ev.brandId(), ev.brandName(),
                        ev.discountPercentage(), ev.ratingAverage(), ev.createdAt());
            }

            log.info("[ES] Re-indexed {} variant(s) for activated productBaseId={}",
                    variants.size(), event.productBaseId());
        } catch (Exception ex) {
            log.error("[ES] Failed to re-index activated productBaseId={}: {}",
                    event.productBaseId(), ex.getMessage());
        }
    }
}
