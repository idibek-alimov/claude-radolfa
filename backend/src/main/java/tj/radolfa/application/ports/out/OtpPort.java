package tj.radolfa.application.ports.out;

/**
 * Out-Port: OTP generation and verification.
 * Infrastructure adapters (in-memory, Redis, SMS) implement this.
 */
public interface OtpPort {

    /**
     * Generates and stores a new OTP for the given phone number.
     */
    String generateOtp(String phone);

    /**
     * Verifies an OTP for the given phone number.
     * If valid, the OTP is consumed (removed from store).
     */
    boolean verifyOtp(String phone, String otp);
}
