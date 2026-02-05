package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.User;

import java.util.Optional;

/**
 * Use case: Verify OTP and issue JWT token.
 *
 * <p>Completes the phone-based authentication flow by verifying the OTP
 * and returning authentication credentials (JWT) upon success.
 */
public interface VerifyOtpUseCase {

    /**
     * Result of OTP verification.
     *
     * @param token the JWT token (present only on success)
     * @param user  the authenticated user (present only on success)
     */
    record AuthResult(String token, User user) {}

    /**
     * Verifies the OTP for the given phone number.
     * On success, creates or retrieves the user and issues a JWT.
     *
     * @param phone the phone number
     * @param otp   the OTP code to verify
     * @return Optional containing auth result if valid, empty if invalid/expired
     */
    Optional<AuthResult> execute(String phone, String otp);
}
