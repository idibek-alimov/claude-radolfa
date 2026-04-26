package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase;
import tj.radolfa.application.ports.out.ExpandCategoryTargetPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadUserSegmentContextPort;
import tj.radolfa.application.ports.out.LoadUserSegmentContextPort.UserSegmentContext;
import tj.radolfa.application.ports.out.QueryDiscountUsagePort;
import tj.radolfa.domain.model.CategoryTarget;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountTarget;
import tj.radolfa.domain.model.Segment;
import tj.radolfa.domain.model.SegmentTarget;
import tj.radolfa.domain.model.SkuTarget;
import tj.radolfa.domain.model.StackingPolicy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResolveDiscountsService implements ResolveDiscountsUseCase {

    private final LoadDiscountPort loadDiscountPort;
    private final ExpandCategoryTargetPort expandCategoryTargetPort;
    private final QueryDiscountUsagePort queryDiscountUsagePort;
    private final LoadUserSegmentContextPort loadUserSegmentContextPort;

    public ResolveDiscountsService(LoadDiscountPort loadDiscountPort,
                                   ExpandCategoryTargetPort expandCategoryTargetPort,
                                   QueryDiscountUsagePort queryDiscountUsagePort,
                                   LoadUserSegmentContextPort loadUserSegmentContextPort) {
        this.loadDiscountPort = loadDiscountPort;
        this.expandCategoryTargetPort = expandCategoryTargetPort;
        this.queryDiscountUsagePort = queryDiscountUsagePort;
        this.loadUserSegmentContextPort = loadUserSegmentContextPort;
    }

    @Override
    public Map<String, List<Discount>> resolve(Query q) {
        if (q.itemCodes().isEmpty()) return Map.of();

        // 1. Load all candidate discounts (SKU-targeted + non-SKU-targeted)
        List<Discount> candidates = loadCandidates(q.itemCodes());
        if (candidates.isEmpty()) return Map.of();

        // 2. Expand category targets to SKU codes (batched once)
        Map<Long, Set<String>> categorySkuMap = expandCategoryTargets(candidates);

        // 3. Load user segment context once
        Optional<UserSegmentContext> userCtx = q.userId() != null
                ? loadUserSegmentContextPort.loadFor(q.userId())
                : Optional.empty();

        // 4. Load usage counts (batched)
        List<Long> candidateIds = candidates.stream().map(Discount::id).toList();
        Map<Long, Long> totalUsage = queryDiscountUsagePort.countByDiscountIds(candidateIds);
        Map<Long, Long> userUsage = q.userId() != null
                ? queryDiscountUsagePort.countByDiscountIdsForUser(candidateIds, q.userId())
                : Map.of();

        Set<String> requestedCodes = new HashSet<>(q.itemCodes());

        // 5. Build per-itemCode eligible discount lists
        Map<String, List<Discount>> eligibleByCode = new HashMap<>();
        for (String code : q.itemCodes()) eligibleByCode.put(code, new ArrayList<>());

        for (Discount d : candidates) {
            if (!passesCouponGate(d, q.couponCode())) continue;
            if (!passesUsageCap(d, totalUsage, userUsage)) continue;
            if (!passesMinBasket(d, q.cartSubtotal())) continue;
            if (!passesSegmentGate(d, userCtx)) continue;

            Set<String> covered = codesForDiscount(d, categorySkuMap, requestedCodes);
            for (String code : covered) {
                eligibleByCode.get(code).add(d);
            }
        }

        // 6. For each item code: pick BEST_WINS winner, then append STACKABLE
        Map<String, List<Discount>> result = new HashMap<>();
        // 1st: lower rank wins. 2nd: larger amountValue wins (Best Price Wins — works for
        // both PERCENT and FIXED; cross-type comparison uses raw value as a reasonable proxy).
        // 3rd: lower ID wins as the final deterministic tiebreaker.
        Comparator<Discount> priority = Comparator
                .comparingInt((Discount d) -> d.type().rank())
                .thenComparing(Comparator.comparing(Discount::amountValue).reversed())
                .thenComparingLong(Discount::id);

        for (String code : q.itemCodes()) {
            List<Discount> eligible = eligibleByCode.get(code);
            if (eligible.isEmpty()) continue;

            List<Discount> bestWins = eligible.stream()
                    .filter(d -> d.type().stackingPolicy() == StackingPolicy.BEST_WINS)
                    .sorted(priority)
                    .toList();

            List<Discount> stackable = eligible.stream()
                    .filter(d -> d.type().stackingPolicy() == StackingPolicy.STACKABLE)
                    .sorted(priority)
                    .toList();

            List<Discount> ordered = new ArrayList<>();
            if (!bestWins.isEmpty()) ordered.add(bestWins.get(0)); // rank-winner only
            ordered.addAll(stackable);

            if (!ordered.isEmpty()) result.put(code, ordered);
        }

        return result;
    }

    // ---- Candidate loading ----

    private List<Discount> loadCandidates(List<String> itemCodes) {
        List<Discount> candidates = new ArrayList<>(loadDiscountPort.findActiveByItemCodes(itemCodes));
        Set<Long> seen = candidates.stream().map(Discount::id).collect(Collectors.toSet());
        for (Discount d : loadDiscountPort.findActiveWithAnyNonSkuTarget()) {
            if (seen.add(d.id())) candidates.add(d);
        }
        return candidates;
    }

    // ---- Category expansion ----

    private Map<Long, Set<String>> expandCategoryTargets(List<Discount> candidates) {
        Map<Long, Boolean> toExpand = new HashMap<>();
        for (Discount d : candidates) {
            for (DiscountTarget t : d.targets()) {
                if (t instanceof CategoryTarget ct) {
                    toExpand.merge(ct.categoryId(), ct.includeDescendants(),
                            (existing, newVal) -> existing || newVal);
                }
            }
        }
        if (toExpand.isEmpty()) return Map.of();

        Map<Long, Set<String>> result = new HashMap<>();
        for (Map.Entry<Long, Boolean> entry : toExpand.entrySet()) {
            List<String> codes = expandCategoryTargetPort.resolveSkuCodes(
                    Map.of(entry.getKey(), entry.getValue()));
            result.put(entry.getKey(), new HashSet<>(codes));
        }
        return result;
    }

    // ---- Gating ----

    private boolean passesUsageCap(Discount d, Map<Long, Long> total, Map<Long, Long> perUser) {
        if (d.usageCapTotal() != null) {
            if (total.getOrDefault(d.id(), 0L) >= d.usageCapTotal()) return false;
        }
        if (d.usageCapPerCustomer() != null && !perUser.isEmpty()) {
            if (perUser.getOrDefault(d.id(), 0L) >= d.usageCapPerCustomer()) return false;
        }
        return true;
    }

    private boolean passesMinBasket(Discount d, BigDecimal cartSubtotal) {
        if (d.minBasketAmount() == null || cartSubtotal == null) return true;
        return cartSubtotal.compareTo(d.minBasketAmount()) >= 0;
    }

    private boolean passesCouponGate(Discount d, String provided) {
        if (d.couponCode() == null) return true;
        return provided != null && d.couponCode().equalsIgnoreCase(provided);
    }

    private boolean passesSegmentGate(Discount d, Optional<UserSegmentContext> userCtx) {
        List<SegmentTarget> segTargets = d.targets().stream()
                .filter(t -> t instanceof SegmentTarget)
                .map(t -> (SegmentTarget) t)
                .toList();

        if (segTargets.isEmpty()) return true;

        for (SegmentTarget st : segTargets) {
            if (st.segment() == Segment.NEW_CUSTOMER) {
                boolean isNew = userCtx.map(UserSegmentContext::isNewCustomer).orElse(true);
                if (isNew) return true;
            } else if (st.segment() == Segment.LOYALTY_TIER) {
                if (userCtx.isEmpty()) continue; // guests never match tier
                Long tierId = userCtx.get().loyaltyTierId();
                if (tierId != null && tierId.toString().equals(st.referenceId())) return true;
            }
        }
        return false;
    }

    // ---- Target → itemCode mapping ----

    private Set<String> codesForDiscount(Discount d,
                                          Map<Long, Set<String>> categorySkuMap,
                                          Set<String> requested) {
        Set<String> covered = new HashSet<>();
        for (DiscountTarget t : d.targets()) {
            if (t instanceof SkuTarget st) {
                if (requested.contains(st.itemCode())) covered.add(st.itemCode());
            } else if (t instanceof CategoryTarget ct) {
                Set<String> catCodes = categorySkuMap.getOrDefault(ct.categoryId(), Set.of());
                for (String c : catCodes) {
                    if (requested.contains(c)) covered.add(c);
                }
            }
            // SegmentTarget is gate-only, does not expand codes
        }
        return covered;
    }
}
