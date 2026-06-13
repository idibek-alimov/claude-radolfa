package tj.radolfa.application.ports.in.user;

/**
 * In-Port: a customer requests to change their phone number.
 *
 * <p>Validates and reserves the new number, then generates and sends an OTP to it.
 * The change is only applied once the OTP is confirmed via {@link ConfirmPhoneChangeUseCase}.
 */
public interface RequestPhoneChangeUseCase {

    void execute(Command command);

    record Command(Long userId, String newPhone) {}
}
