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
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.service.LoyaltyCalculator;

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
 *   <li>Persist the updated user.</li>
 * </ol>
 */
@Service
@Transactional
public class AwardLoyaltyPointsService implements AwardLoyaltyPointsUseCase {

    private final LoadUserPort         loadUserPort;
    private final LoadOrderPort        loadOrderPort;
    private final LoadLoyaltyTierPort  loadLoyaltyTierPort;
    private final SaveUserPort         saveUserPort;
    private final SaveOrderPort        saveOrderPort;
    private final LoyaltyCalculator    loyaltyCalculator;

    public AwardLoyaltyPointsService(LoadUserPort loadUserPort,
                                     LoadOrderPort loadOrderPort,
                                     LoadLoyaltyTierPort loadLoyaltyTierPort,
                                     SaveUserPort saveUserPort,
                                     SaveOrderPort saveOrderPort,
                                     LoyaltyCalculator loyaltyCalculator) {
        this.loadUserPort        = loadUserPort;
        this.loadOrderPort       = loadOrderPort;
        this.loadLoyaltyTierPort = loadLoyaltyTierPort;
        this.saveUserPort        = saveUserPort;
        this.saveOrderPort       = saveOrderPort;
        this.loyaltyCalculator   = loyaltyCalculator;
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

        int earnedPoints = loyaltyCalculator.computeEarnedPoints(user.loyalty(), order.totalAmount());
        LoyaltyProfile updated = loyaltyCalculator.awardPoints(
                user.loyalty(), order.totalAmount(), allTiers);

        saveUserPort.save(new User(
                user.id(), user.phone(), user.role(), user.name(),
                user.email(), updated, user.enabled(), user.version()));

        // Record awarded points on the order so RefundPaymentService can revoke them later
        Order recorded = new Order(
                order.id(), order.userId(), order.externalOrderId(),
                order.status(), order.totalAmount(), order.items(), order.createdAt(),
                order.loyaltyPointsRedeemed(), earnedPoints);
        saveOrderPort.save(recorded);
    }
}
