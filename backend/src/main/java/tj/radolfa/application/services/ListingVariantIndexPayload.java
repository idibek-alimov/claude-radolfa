package tj.radolfa.application.services;

import org.springframework.stereotype.Component;
import tj.radolfa.application.event.ListingVariantIndexedEvent;
import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

import java.math.BigDecimal;
import java.util.List;

/**
 * Builds a {@link ListingVariantIndexedEvent} from a domain variant + its owning base.
 * Centralises the price/stock/sku-codes/color-hex derivation used by every index-event publisher.
 */
@Component
public class ListingVariantIndexPayload {

    private final LoadSkuPort   loadSkuPort;
    private final LoadColorPort loadColorPort;

    public ListingVariantIndexPayload(LoadSkuPort loadSkuPort, LoadColorPort loadColorPort) {
        this.loadSkuPort   = loadSkuPort;
        this.loadColorPort = loadColorPort;
    }

    public ListingVariantIndexedEvent build(ListingVariant variant, ProductBase base) {
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

        return new ListingVariantIndexedEvent(
                variant.getId(), variant.getProductBaseId(), variant.getSlug(),
                base.getName(), base.getCategory(), variant.getColorKey(), colorHexCode,
                variant.getWebDescription(), variant.getImages(),
                minPrice, totalStock, variant.getLastSyncAt(),
                variant.getProductCode(), skuCodes,
                base.getStatus() != null ? base.getStatus().name() : null);
    }
}
