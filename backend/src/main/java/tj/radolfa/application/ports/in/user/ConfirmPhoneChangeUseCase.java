package tj.radolfa.application.ports.in.user;

import tj.radolfa.domain.model.User;

/**
 * In-Port: confirm a pending phone-number change with the OTP sent to the new number.
 *
 * <p>On success the user's phone is updated and the refreshed {@link User} is returned.
 */
public interface ConfirmPhoneChangeUseCase {

    User execute(Command command);

    record Command(Long userId, String newPhone, String otp) {}
}
