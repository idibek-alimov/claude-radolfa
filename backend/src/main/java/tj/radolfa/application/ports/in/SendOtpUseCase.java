package tj.radolfa.application.ports.in;

/**
 * Use case: Send OTP to a phone number for authentication.
 *
 * <p>This initiates the phone-based authentication flow by generating
 * and "sending" (in DEV: logging) an OTP to the user's phone number.
 */
public interface SendOtpUseCase {

    /**
     * Generates and sends an OTP to the given phone number.
     *
     * @param phone the phone number to send OTP to
     */
    void execute(String phone);
}
