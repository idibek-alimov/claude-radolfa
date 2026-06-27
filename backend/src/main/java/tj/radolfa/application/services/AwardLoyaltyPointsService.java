package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.AwardLoyaltyPointsUseCase;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyReason;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.service.LoyaltyCalculator;
import tj.radolfa.infrastructure.config.LoyaltyRewardProperties;

import java.util.List;

/**
 * Awards cashback points to a user after a successful payment.
 *
 * <p>This implementation supersedes {@link AwardLoyaltyPointsStub} automatically
 * because the stub uses {@code @ConditionalOnMissingBean}.
 *
 * <p>Steps:
 * <ol>
 *   <li>Load the user and their current {@link LoyaltyProfile}.</li>
 *   <li>Load the paid order for its {@code totalAmount}.</li>
 *   <li>Load all tier definitions for tier-upgrade logic.</li>
 *   <li>Delegate to {@link LoyaltyCalculator#awardPoints} to compute new profile.</li>
 *   <li>Append an {@code EARN_CASHBACK} ledger lot (if points earned &gt; 0).</li>
 *   <li>Persist the updated user and record the awarded count on the order.</li>
 * </ol>
 */
@Service
@Transactional
public class AwardLoyaltyPointsService implements AwardLoyaltyPointsUseCase {

    private final LoadUserPort           loadUserPort;
    private final LoadOrderPort          loadOrderPort;
    private final LoadLoyaltyTierPort    loadLoyaltyTierPort;
    private final SaveUserPort           saveUserPort;
    private final SaveOrderPort          saveOrderPort;
    private final LoyaltyCalculator      loyaltyCalculator;
    private final LoyaltyLedgerWriter    ledgerWriter;
    private final LoyaltyRewardProperties properties;

    public AwardLoyaltyPointsService(LoadUserPort loadUserPort,
                                     LoadOrderPort loadOrderPort,
                                     LoadLoyaltyTierPort loadLoyaltyTierPort,
                                     SaveUserPort saveUserPort,
                                     SaveOrderPort saveOrderPort,
                                     LoyaltyCalculator loyaltyCalculator,
                                     LoyaltyLedgerWriter ledgerWriter,
                                     LoyaltyRewardProperties properties) {
        this.loadUserPort        = loadUserPort;
        this.loadOrderPort       = loadOrderPort;
        this.loadLoyaltyTierPort = loadLoyaltyTierPort;
        this.saveUserPort        = saveUserPort;
        this.saveOrderPort       = saveOrderPort;
        this.loyaltyCalculator   = loyaltyCalculator;
        this.ledgerWriter        = ledgerWriter;
        this.properties          = properties;
    }

    @Override
    public void execute(Long userId, Long orderId) {
        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        if (order.loyaltyPointsAwarded() > 0) {
            return; // already awarded — idempotency guard
        }

        List<LoyaltyTier> allTiers = loadLoyaltyTierPort.findAll();

        int oldPoints    = user.loyalty().points();
        int earnedPoints = loyaltyCalculator.computeEarnedPoints(user.loyalty(), order.totalAmount());
        LoyaltyProfile updated = loyaltyCalculator.awardPoints(
                user.loyalty(), order.totalAmount(), allTiers);

        // Append a credit lot; skip noise row when cashback rate yields 0 points
        if (earnedPoints > 0) {
            ledgerWriter.credit(userId, earnedPoints, LoyaltyReason.EARN_CASHBACK,
                    orderId, null, oldPoints, properties.pointsTtlMonths());
        }

        saveUserPort.save(new User(
                user.id(), user.phone(), user.role(), user.name(),
                user.email(), updated, user.enabled(), user.version()));

        // Record awarded points on the order so RefundPaymentService can revoke them later
        Order recorded = order.toBuilder()
                .loyaltyPointsAwarded(earnedPoints)
                .build();
        saveOrderPort.save(recorded);
    }
}
