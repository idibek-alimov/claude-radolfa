package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.RegisterUserUseCase;
import tj.radolfa.application.ports.in.SendOtpUseCase;
import tj.radolfa.application.ports.in.VerifyOtpUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.OtpPort;
import tj.radolfa.application.ports.out.TokenIssuerPort;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;

import java.util.Optional;

/**
 * Application service implementing phone-based OTP authentication.
 *
 * <p>
 * Flow:
 * <ol>
 * <li>User requests OTP via {@link #execute(String)} (SendOtpUseCase)</li>
 * <li>OTP is generated and logged (DEV) or sent via SMS (PROD)</li>
 * <li>User verifies OTP via {@link #execute(String, String)}
 * (VerifyOtpUseCase)</li>
 * <li>If valid, user is created (first login) or retrieved, JWT is issued</li>
 * </ol>
 *
 * <p>
 * New users are automatically assigned the {@code USER} role.
 */
@Service
public class OtpAuthService implements SendOtpUseCase, VerifyOtpUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(OtpAuthService.class);

    private final OtpPort otpPort;
    private final TokenIssuerPort tokenIssuer;
    private final LoadUserPort loadUserPort;
    private final RegisterUserUseCase registerUserUseCase;

    public OtpAuthService(OtpPort otpPort,
            TokenIssuerPort tokenIssuer,
            LoadUserPort loadUserPort,
            RegisterUserUseCase registerUserUseCase) {
        this.otpPort = otpPort;
        this.tokenIssuer = tokenIssuer;
        this.loadUserPort = loadUserPort;
        this.registerUserUseCase = registerUserUseCase;
    }

    // ----------------------------------------------------------------
    // SendOtpUseCase
    // ----------------------------------------------------------------

    @Override
    public void execute(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number must not be null or blank");
        }
        PhoneNumber normalized = PhoneNumber.of(phone);
        otpPort.generateOtp(normalized.value());
        LOG.info("[AUTH] OTP sent to phone={}", mask(normalized));
    }

    // ----------------------------------------------------------------
    // VerifyOtpUseCase
    // ----------------------------------------------------------------

    @Override
    @Transactional
    public Optional<AuthResult> execute(String phone, String otp) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number must not be null or blank");
        }
        PhoneNumber normalized = PhoneNumber.of(phone);

        if (!otpPort.verifyOtp(normalized.value(), otp)) {
            LOG.warn("[AUTH] OTP verification failed for phone={}", mask(normalized));
            return Optional.empty();
        }

        // Find existing user or self-register on first login
        User user = loadUserPort.loadByPhone(normalized.value())
                .orElseGet(() -> registerUserUseCase.execute(normalized));

        if (!user.enabled()) {
            LOG.warn("[AUTH] Blocked user attempted login: phone={}", mask(normalized));
            return Optional.empty();
        }

        // Generate JWT
        String token = tokenIssuer.generateToken(user.id(), user.phone().value(), user.role());
        LOG.info("[AUTH] User authenticated: phone={}, role={}", mask(user.phone()), user.role());

        return Optional.of(new AuthResult(token, user));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static String mask(PhoneNumber phone) {
        if (phone == null) return "***";
        String v = phone.value();
        if (v.length() <= 4) return "***";
        return v.substring(0, v.length() - 4).replaceAll("\\d", "*")
                + v.substring(v.length() - 4);
    }
}
