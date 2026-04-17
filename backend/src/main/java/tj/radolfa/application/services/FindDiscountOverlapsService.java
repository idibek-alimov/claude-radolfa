package tj.radolfa.application.services;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.FindDiscountOverlapsUseCase;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountOverlapRow;
import tj.radolfa.domain.model.DiscountSummary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class FindDiscountOverlapsService implements FindDiscountOverlapsUseCase {

    private final LoadDiscountPort loadDiscountPort;

    public FindDiscountOverlapsService(LoadDiscountPort loadDiscountPort) {
        this.loadDiscountPort = loadDiscountPort;
    }

    @Override
    public List<DiscountOverlapRow> execute() {
        DiscountFilter activeFilter = new DiscountFilter(null, "ACTIVE", null, null, null);
        List<Discount> allActive = loadDiscountPort
                .findAll(activeFilter, PageRequest.of(0, 5000))
                .getContent();

        // Build map: itemCode → list of discounts covering that code
        Map<String, List<Discount>> byCode = new HashMap<>();
        for (Discount d : allActive) {
            for (String code : d.itemCodes()) {
                byCode.computeIfAbsent(code, k -> new ArrayList<>()).add(d);
            }
        }

        List<DiscountOverlapRow> overlaps = new ArrayList<>();
        for (Map.Entry<String, List<Discount>> entry : byCode.entrySet()) {
            List<Discount> campaigns = entry.getValue();
            if (campaigns.size() < 2) continue;

            // Winner: lowest rank, then lowest id for tie-breaking
            Discount winner = campaigns.stream()
                    .min(Comparator.comparingInt((Discount d) -> d.type().rank())
                            .thenComparingLong(Discount::id))
                    .orElseThrow();

            List<DiscountSummary> losers = campaigns.stream()
                    .filter(d -> !d.id().equals(winner.id()))
                    .map(d -> new DiscountSummary(d.id(), d.title(), d.colorHex(), d.discountValue(), d.type()))
                    .toList();

            overlaps.add(new DiscountOverlapRow(
                    entry.getKey(),
                    new DiscountSummary(winner.id(), winner.title(), winner.colorHex(), winner.discountValue(), winner.type()),
                    losers
            ));
        }

        return overlaps;
    }
}
