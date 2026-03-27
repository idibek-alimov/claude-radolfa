package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.loyalty.AwardLoyaltyPointsUseCase;

/**
 * No-op stub for {@link AwardLoyaltyPointsUseCase}.
 *
 * <p>Active only when no other implementation is present in the context.
 * Phase 9 will replace this with {@code AwardLoyaltyPointsService},
 * which will automatically take precedence via {@code @ConditionalOnMissingBean}.
 */
@Service
@ConditionalOnMissingBean(value = AwardLoyaltyPointsUseCase.class,
        ignored = AwardLoyaltyPointsStub.class)
public class AwardLoyaltyPointsStub implements AwardLoyaltyPointsUseCase {

    private static final Logger log = LoggerFactory.getLogger(AwardLoyaltyPointsStub.class);

    @Override
    public void execute(Long userId, Long orderId) {
        log.info("[LOYALTY STUB] award points skipped — not implemented yet (Phase 9). userId={} orderId={}",
                userId, orderId);
    }
}
