package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.cart.ApplyCouponUseCase;
import tj.radolfa.application.ports.in.cart.GetCartUseCase;
import tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.QueryDiscountUsagePort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.readmodel.CartView;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.Discount;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ApplyCouponService implements ApplyCouponUseCase {

    private final LoadDiscountPort loadDiscountPort;
    private final LoadCartPort loadCartPort;
    private final SaveCartPort saveCartPort;
    private final LoadSkuPort loadSkuPort;
    private final ResolveDiscountsUseCase resolveDiscountsUseCase;
    private final QueryDiscountUsagePort queryDiscountUsagePort;
    private final GetCartUseCase getCartUseCase;

    public ApplyCouponService(LoadDiscountPort loadDiscountPort,
                              LoadCartPort loadCartPort,
                              SaveCartPort saveCartPort,
                              LoadSkuPort loadSkuPort,
                              ResolveDiscountsUseCase resolveDiscountsUseCase,
                              QueryDiscountUsagePort queryDiscountUsagePort,
                              GetCartUseCase getCartUseCase) {
        this.loadDiscountPort      = loadDiscountPort;
        this.loadCartPort          = loadCartPort;
        this.saveCartPort          = saveCartPort;
        this.loadSkuPort           = loadSkuPort;
        this.resolveDiscountsUseCase = resolveDiscountsUseCase;
        this.queryDiscountUsagePort = queryDiscountUsagePort;
        this.getCartUseCase        = getCartUseCase;
    }

    @Override
    @Transactional
    public Result execute(Long userId, String couponCode) {
        Cart cart = loadCartPort.findActiveByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No active cart for user: " + userId));

        Discount d = loadDiscountPort.findByCouponCode(couponCode)
                .orElse(null);
        if (d == null) return invalid("NOT_FOUND");

        if (d.disabled()) return invalid("DISABLED");
        if (!d.isActive(Instant.now())) return invalid("NOT_ACTIVE");

        if (d.usageCapTotal() != null) {
            long used = queryDiscountUsagePort.countByDiscountIds(List.of(d.id()))
                    .getOrDefault(d.id(), 0L);
            if (used >= d.usageCapTotal()) return invalid("CAP_EXHAUSTED");
        }

        List<String> itemCodes = loadSkuPort
                .findAllByIdsAsMap(cart.getItems().stream().map(i -> i.getSkuId()).toList())
                .values().stream()
                .map(sku -> sku.getSkuCode())
                .distinct()
                .toList();

        Map<String, List<Discount>> resolved = resolveDiscountsUseCase.resolve(
                new ResolveDiscountsUseCase.Query(itemCodes, userId, cart.total().amount(), couponCode));

        List<String> affectedSkus = resolved.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(disc -> disc.id().equals(d.id())))
                .map(Map.Entry::getKey)
                .toList();

        if (affectedSkus.isEmpty()) return invalid("NO_ITEMS_AFFECTED");

        cart.applyCoupon(couponCode);
        saveCartPort.save(cart);

        CartView refreshed = getCartUseCase.execute(userId);
        return new Result(true, d.id(), affectedSkus, null, refreshed);
    }

    private Result invalid(String reason) {
        return new Result(false, null, List.of(), reason, null);
    }
}
