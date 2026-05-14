package tj.radolfa.infrastructure.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tj.radolfa.application.event.ListingVariantIndexedEvent;
import tj.radolfa.application.ports.out.ListingIndexPort;

@Slf4j
@Component
public class ListingIndexEventListener {

    private final ListingIndexPort listingIndexPort;

    public ListingIndexEventListener(ListingIndexPort listingIndexPort) {
        this.listingIndexPort = listingIndexPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVariantIndexed(ListingVariantIndexedEvent event) {
        try {
            listingIndexPort.index(
                    event.variantId(), event.productBaseId(), event.slug(),
                    event.name(), event.category(), event.colorKey(), event.colorHexCode(),
                    event.description(), event.images(), event.price(), event.totalStock(),
                    event.lastSyncAt(), event.productCode(), event.skuCodes());
        } catch (Exception ex) {
            log.error("[ES] Indexing failed for variant slug={}: {}", event.slug(), ex.getMessage());
        }
    }
}
