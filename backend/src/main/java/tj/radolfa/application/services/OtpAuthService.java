package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.SendOtpUseCase;
import tj.radolfa.application.ports.in.VerifyOtpUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.infrastructure.security.JwtUtil;
import tj.radolfa.infrastructure.security.OtpStore;

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

    private final OtpStore otpStore;
    private final JwtUtil jwtUtil;
    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    public OtpAuthService(OtpStore otpStore,
            JwtUtil jwtUtil,
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort) {
        this.otpStore = otpStore;
        this.jwtUtil = jwtUtil;
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    // ----------------------------------------------------------------
    // SendOtpUseCase
    // ----------------------------------------------------------------

    @Override
    public void execute(String phone) {
        PhoneNumber normalized = PhoneNumber.of(phone);
        otpStore.generateOtp(normalized.value());
        LOG.info("[AUTH] OTP sent to phone={}", normalized);
    }

    // ----------------------------------------------------------------
    // VerifyOtpUseCase
    // ----------------------------------------------------------------

    @Override
    @Transactional
    public Optional<AuthResult> execute(String phone, String otp) {
        PhoneNumber normalized = PhoneNumber.of(phone);

        if (!otpStore.verifyOtp(normalized.value(), otp)) {
            LOG.warn("[AUTH] OTP verification failed for phone={}", normalized);
            return Optional.empty();
        }

        // Find existing user or create new one
        User user = loadUserPort.loadByPhone(normalized.value())
                .orElseGet(() -> createNewUser(normalized));

        // Generate JWT
        String token = jwtUtil.generateToken(user.id(), user.phone().value(), user.role());
        LOG.info("[AUTH] User authenticated: phone={}, role={}", user.phone(), user.role());

        return Optional.of(new AuthResult(token, user));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /**
     * Creates a new user with default USER role.
     */
    private User createNewUser(PhoneNumber phone) {
        User newUser = new User(null, phone, UserRole.USER, null, null, 0, null);
        User saved = saveUserPort.save(newUser);
        LOG.info("[AUTH] Created new user: phone={}, id={}", phone, saved.id());
        return saved;
    }
}
