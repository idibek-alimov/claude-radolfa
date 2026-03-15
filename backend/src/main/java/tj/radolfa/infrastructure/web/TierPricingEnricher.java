package tj.radolfa.infrastructure.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import tj.radolfa.application.ports.in.ResolveUserDiscountUseCase;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.CollectionPageDto;
import tj.radolfa.infrastructure.web.dto.HomeSectionDto;
import tj.radolfa.infrastructure.web.dto.ListingVariantDetailDto;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Enriches product DTOs with tier-based pricing.
 * Resolves the authenticated user's discount percentage from the security context
 * and applies it to product price fields.
 */
@Component
@RequestScope
public class TierPricingEnricher {

    private final ResolveUserDiscountUseCase resolveUserDiscountUseCase;
    private BigDecimal cachedDiscount;

    public TierPricingEnricher(ResolveUserDiscountUseCase resolveUserDiscountUseCase) {
        this.resolveUserDiscountUseCase = resolveUserDiscountUseCase;
    }

    public BigDecimal resolveDiscount() {
        if (cachedDiscount != null) return cachedDiscount;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof JwtAuthenticatedUser principal)) {
            cachedDiscount = BigDecimal.ZERO;
        } else {
            cachedDiscount = resolveUserDiscountUseCase.resolveForUser(principal.userId());
        }
        return cachedDiscount;
    }

    public PageResult<ListingVariantDto> enrich(PageResult<ListingVariantDto> page) {
        BigDecimal discount = resolveDiscount();
        if (discount.compareTo(BigDecimal.ZERO) == 0) return page;

        List<ListingVariantDto> enriched = page.items().stream()
                .map(dto -> dto.withLoyaltyPrice(discount))
                .toList();
        return new PageResult<>(enriched, page.totalElements(), page.page(), page.hasMore());
    }

    public ListingVariantDetailDto enrich(ListingVariantDetailDto detail) {
        BigDecimal discount = resolveDiscount();
        if (discount.compareTo(BigDecimal.ZERO) == 0) return detail;
        return detail.withLoyaltyPrice(discount);
    }

    public List<HomeSectionDto> enrichSections(List<HomeSectionDto> sections) {
        BigDecimal discount = resolveDiscount();
        if (discount.compareTo(BigDecimal.ZERO) == 0) return sections;

        return sections.stream()
                .map(s -> new HomeSectionDto(s.key(), s.title(),
                        s.items().stream().map(dto -> dto.withLoyaltyPrice(discount)).toList()))
                .toList();
    }

    public CollectionPageDto enrich(CollectionPageDto cp) {
        BigDecimal discount = resolveDiscount();
        if (discount.compareTo(BigDecimal.ZERO) == 0) return cp;

        PageResult<ListingVariantDto> page = cp.page();
        List<ListingVariantDto> enriched = page.items().stream()
                .map(dto -> dto.withLoyaltyPrice(discount))
                .toList();
        return new CollectionPageDto(cp.key(), cp.title(),
                new PageResult<>(enriched, page.totalElements(), page.page(), page.hasMore()));
    }
}
