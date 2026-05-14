package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;

/**
 * In-Port: create a new application user without any external system.
 *
 * <p>Called internally by {@code VerifyOtpUseCase} when a phone number is
 * verified for the first time and no matching user exists yet.
 *
 * <p>The newly created user is assigned {@code UserRole.USER} and an empty
 * {@link tj.radolfa.domain.model.LoyaltyProfile}.
 */
public interface RegisterUserUseCase {

    /**
     * @param phone the verified phone number
     * @return the persisted {@link User} record
     */
    User execute(PhoneNumber phone);
}
