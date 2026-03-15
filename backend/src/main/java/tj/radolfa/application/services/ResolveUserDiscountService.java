package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.ResolveUserDiscountUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.User;

import java.math.BigDecimal;

@Service
public class ResolveUserDiscountService implements ResolveUserDiscountUseCase {

    private final LoadUserPort loadUserPort;

    public ResolveUserDiscountService(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    @Override
    public BigDecimal resolveForUser(Long userId) {
        return loadUserPort.loadById(userId)
                .map(User::loyalty)
                .filter(lp -> lp != null && lp.tier() != null)
                .map(lp -> lp.tier().discountPercentage())
                .orElse(BigDecimal.ZERO);
    }
}
