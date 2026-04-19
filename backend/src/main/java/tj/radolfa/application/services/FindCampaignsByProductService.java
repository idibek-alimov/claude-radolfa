package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.FindCampaignsByProductUseCase;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountSummary;
import tj.radolfa.domain.model.ListingVariant;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FindCampaignsByProductService implements FindCampaignsByProductUseCase {

    private final LoadListingVariantPort loadListingVariantPort;
    private final LoadSkuPort loadSkuPort;
    private final LoadDiscountPort loadDiscountPort;

    public FindCampaignsByProductService(LoadListingVariantPort loadListingVariantPort,
                                         LoadSkuPort loadSkuPort,
                                         LoadDiscountPort loadDiscountPort) {
        this.loadListingVariantPort = loadListingVariantPort;
        this.loadSkuPort = loadSkuPort;
        this.loadDiscountPort = loadDiscountPort;
    }

    @Override
    public List<DiscountSummary> execute(Long productBaseId) {
        List<ListingVariant> variants = loadListingVariantPort.findAllByProductBaseId(productBaseId);
        if (variants.isEmpty()) return List.of();

        List<String> skuCodes = variants.stream()
                .flatMap(v -> loadSkuPort.findSkusByVariantId(v.getId()).stream())
                .map(sku -> sku.getSkuCode())
                .distinct()
                .toList();

        if (skuCodes.isEmpty()) return List.of();

        List<Discount> discounts = loadDiscountPort.findActiveByItemCodes(skuCodes);

        // Dedupe by id, sort by type rank ASC then id ASC
        Map<Long, Discount> deduped = discounts.stream()
                .collect(Collectors.toMap(
                        Discount::id,
                        d -> d,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return deduped.values().stream()
                .sorted(Comparator.comparingInt((Discount d) -> d.type().rank())
                        .thenComparingLong(Discount::id))
                .map(d -> new DiscountSummary(d.id(), d.title(), d.colorHex(), d.amountValue(), d.amountType(), d.type()))
                .toList();
    }
}
