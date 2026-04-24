package tj.radolfa.infrastructure.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase;
import tj.radolfa.domain.model.AppliedDiscount;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request-scoped cache for discount resolution.
 * Resolves once per unique set of item codes per request, avoiding N+1 resolution.
 * Computes actual reduced unit prices using SKU original prices from the database.
 */
@Component
@RequestScope
public class DiscountResolutionContext {

    private final ResolveDiscountsUseCase resolveDiscountsUseCase;
    private final SkuRepository skuRepository;

    private Long cachedUserId;
    private boolean userIdResolved;
    private Map<String, List<AppliedDiscount>> cachedResult;
    private List<String> cachedItemCodes;

    public DiscountResolutionContext(ResolveDiscountsUseCase resolveDiscountsUseCase,
                                     SkuRepository skuRepository) {
        this.resolveDiscountsUseCase = resolveDiscountsUseCase;
        this.skuRepository = skuRepository;
    }

    /**
     * Resolves discounts for the given item codes at listing time (no cart subtotal).
     * Memoized per request by item-code list equality.
     */
    public Map<String, List<AppliedDiscount>> resolveForListing(List<String> itemCodes) {
        return resolve(itemCodes, null);
    }

    /**
     * Resolves discounts for checkout — includes cart subtotal for min-basket enforcement.
     * Not memoized (checkout is a one-shot path per request).
     */
    public Map<String, List<AppliedDiscount>> resolveForCheckout(List<String> itemCodes,
                                                                   BigDecimal cartSubtotal) {
        Long userId = resolveUserId();
        Map<String, List<Discount>> ordered = resolveDiscountsUseCase.resolve(
                new ResolveDiscountsUseCase.Query(itemCodes, userId, cartSubtotal, null));
        return foldWithPrices(ordered, itemCodes);
    }

    // ---- Internal ----

    private Map<String, List<AppliedDiscount>> resolve(List<String> itemCodes, BigDecimal cartSubtotal) {
        if (cachedItemCodes != null && cachedItemCodes.equals(itemCodes) && cachedResult != null) {
            return cachedResult;
        }
        Long userId = resolveUserId();
        Map<String, List<Discount>> ordered = resolveDiscountsUseCase.resolve(
                new ResolveDiscountsUseCase.Query(itemCodes, userId, cartSubtotal, null));
        Map<String, List<AppliedDiscount>> result = foldWithPrices(ordered, itemCodes);
        cachedItemCodes = itemCodes;
        cachedResult = result;
        return result;
    }

    private Map<String, List<AppliedDiscount>> foldWithPrices(Map<String, List<Discount>> ordered,
                                                               List<String> itemCodes) {
        if (ordered.isEmpty()) return Map.of();

        // Load original prices for item codes that have discounts
        Map<String, BigDecimal> priceByCode = new HashMap<>();
        skuRepository.findBySkuCodeIn(ordered.keySet()).stream()
                .filter(s -> s.getOriginalPrice() != null)
                .forEach(s -> priceByCode.put(s.getSkuCode(), s.getOriginalPrice()));

        Map<String, List<AppliedDiscount>> result = new HashMap<>();
        for (Map.Entry<String, List<Discount>> entry : ordered.entrySet()) {
            BigDecimal price = priceByCode.get(entry.getKey());
            if (price == null) continue; // SKU not found or no price
            result.put(entry.getKey(), AppliedDiscount.fold(entry.getValue(), price));
        }
        return result;
    }

    private Long resolveUserId() {
        if (userIdResolved) return cachedUserId;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof JwtAuthenticatedUser principal) {
            cachedUserId = principal.userId();
        }
        userIdResolved = true;
        return cachedUserId;
    }
}
