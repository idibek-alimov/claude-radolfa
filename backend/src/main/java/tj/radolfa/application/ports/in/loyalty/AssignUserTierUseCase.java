package tj.radolfa.application.ports.in.loyalty;

import tj.radolfa.domain.model.User;

/**
 * In-Port: manually assign a loyalty tier to a user.
 *
 * <p>Available to both MANAGER and ADMIN. Any tier can be assigned in one step.
 * If the user has no floor tier yet, the entry-level tier (min displayOrder)
 * is recorded as {@code lowestTierEver}.
 */
public interface AssignUserTierUseCase {

    record Command(Long userId, Long tierId) {}

    User execute(Command command);
}
