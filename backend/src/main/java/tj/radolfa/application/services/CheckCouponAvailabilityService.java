package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.discount.CheckCouponAvailabilityUseCase;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.domain.model.Discount;

import java.util.Optional;

@Service
public class CheckCouponAvailabilityService implements CheckCouponAvailabilityUseCase {

    private final LoadDiscountPort loadDiscountPort;

    public CheckCouponAvailabilityService(LoadDiscountPort loadDiscountPort) {
        this.loadDiscountPort = loadDiscountPort;
    }

    @Override
    public boolean isAvailable(String couponCode, Long excludeDiscountId) {
        Optional<Discount> found = loadDiscountPort.findByCouponCode(couponCode);
        if (found.isEmpty()) return true;
        return excludeDiscountId != null && found.get().id().equals(excludeDiscountId);
    }
}
