package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.RegisterUserUseCase;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

/**
 * Creates a new application user on first OTP verification.
 *
 * <p>Assigned defaults:
 * <ul>
 *   <li>role = {@link UserRole#USER}</li>
 *   <li>enabled = {@code true}</li>
 *   <li>loyalty = {@link LoyaltyProfile#empty()}</li>
 *   <li>name, email = {@code null} (filled in via profile update)</li>
 * </ul>
 */
@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterUserService.class);

    private final SaveUserPort saveUserPort;

    public RegisterUserService(SaveUserPort saveUserPort) {
        this.saveUserPort = saveUserPort;
    }

    @Override
    @Transactional
    public User execute(PhoneNumber phone) {
        User newUser = new User(null, phone, UserRole.USER, null, null, LoyaltyProfile.empty(), true, null);
        User saved = saveUserPort.save(newUser);
        LOG.info("[REGISTER] New user created: phone={}, id={}", mask(phone), saved.id());
        return saved;
    }

    private static String mask(PhoneNumber phone) {
        String v = phone.value();
        if (v.length() <= 4) return "***";
        return v.substring(0, v.length() - 4).replaceAll("\\d", "*") + v.substring(v.length() - 4);
    }
}
