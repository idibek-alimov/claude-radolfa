package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.ResolveUserDiscountUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.User;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ResolveUserDiscountService implements ResolveUserDiscountUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ResolveUserDiscountService.class);

    private final LoadUserPort loadUserPort;

    public ResolveUserDiscountService(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    @Override
    public BigDecimal resolveForUser(Long userId) {
        Optional<User> user = loadUserPort.loadById(userId);
        if (user.isEmpty()) {
            LOG.warn("[LOYALTY] User not found for discount resolution: userId={}", userId);
            return BigDecimal.ZERO;
        }
        return user.map(User::loyalty)
                .filter(lp -> lp != null && lp.tier() != null)
                .map(lp -> lp.tier().discountPercentage())
                .orElse(BigDecimal.ZERO);
    }
}
