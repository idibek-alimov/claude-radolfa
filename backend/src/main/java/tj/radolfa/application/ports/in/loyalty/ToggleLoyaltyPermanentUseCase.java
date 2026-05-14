package tj.radolfa.application.ports.in.loyalty;

import tj.radolfa.domain.model.User;

/**
 * In-Port: lock or unlock a user's loyalty tier from monthly auto-evaluation.
 *
 * <p>ADMIN only. When {@code permanent = true}, the monthly evaluation job skips
 * this user entirely — their tier remains frozen until the flag is cleared.
 */
public interface ToggleLoyaltyPermanentUseCase {

    User execute(Long userId, boolean permanent);
}
