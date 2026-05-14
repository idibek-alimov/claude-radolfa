package tj.radolfa.application.ports.in;

import java.math.BigDecimal;

public interface ResolveUserDiscountUseCase {
    BigDecimal resolveForUser(Long userId);
}
