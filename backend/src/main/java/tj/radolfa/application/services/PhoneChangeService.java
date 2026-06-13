package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.user.ConfirmPhoneChangeUseCase;
import tj.radolfa.application.ports.in.user.RequestPhoneChangeUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.OtpPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;

/**
 * Application service implementing the "change my phone number" flow.
 *
 * <p>
 * Flow:
 * <ol>
 * <li>{@link #execute(RequestPhoneChangeUseCase.Command)} — validate the new number,
 * reject if already taken by another user, then generate and send an OTP to it
 * (reusing {@link OtpPort}, the same machinery as login).</li>
 * <li>{@link #execute(ConfirmPhoneChangeUseCase.Command)} — verify the OTP, re-check
 * uniqueness, and persist the new phone number on the caller's account.</li>
 * </ol>
 *
 * <p>
 * Real SMS delivery of the OTP is still platform-stubbed (see
 * {@link NotificationPort#sendOtpCode}); the demo code {@code "1234"} from
 * {@code OtpStore} works while that is the case.
 */
@Service
public class PhoneChangeService implements RequestPhoneChangeUseCase, ConfirmPhoneChangeUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(PhoneChangeService.class);

    private final OtpPort otpPort;
    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final NotificationPort notificationPort;

    public PhoneChangeService(OtpPort otpPort,
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            NotificationPort notificationPort) {
        this.otpPort = otpPort;
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.notificationPort = notificationPort;
    }

    // ----------------------------------------------------------------
    // RequestPhoneChangeUseCase
    // ----------------------------------------------------------------

    @Override
    @Transactional
    public void execute(RequestPhoneChangeUseCase.Command command) {
        PhoneNumber newPhone = PhoneNumber.of(command.newPhone());

        User caller = loadUserPort.loadById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));

        if (newPhone.value().equals(caller.phone().value())) {
            throw new IllegalArgumentException("This is already your phone number");
        }

        loadUserPort.loadByPhone(newPhone.value()).ifPresent(existing -> {
            if (!existing.id().equals(caller.id())) {
                throw new DuplicateResourceException("Phone number is already in use");
            }
        });

        String code = otpPort.generateOtp(newPhone.value());
        notificationPort.sendOtpCode(newPhone.value(), code);

        LOG.info("[PHONE_CHANGE] OTP sent to new phone={} for userId={}", mask(newPhone), caller.id());
    }

    // ----------------------------------------------------------------
    // ConfirmPhoneChangeUseCase
    // ----------------------------------------------------------------

    @Override
    @Transactional
    public User execute(ConfirmPhoneChangeUseCase.Command command) {
        PhoneNumber newPhone = PhoneNumber.of(command.newPhone());

        if (!otpPort.verifyOtp(newPhone.value(), command.otp())) {
            LOG.warn("[PHONE_CHANGE] OTP verification failed for phone={}", mask(newPhone));
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        User caller = loadUserPort.loadById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));

        loadUserPort.loadByPhone(newPhone.value()).ifPresent(existing -> {
            if (!existing.id().equals(caller.id())) {
                throw new DuplicateResourceException("Phone number is already in use");
            }
        });

        User updated = new User(caller.id(), newPhone, caller.role(), caller.name(), caller.email(),
                caller.loyalty(), caller.enabled(), caller.version(),
                caller.vehicleType(), caller.maxPayloadKg(), caller.maxLengthCm(), caller.maxWidthCm(),
                caller.maxHeightCm(), caller.pickpointId(), caller.deliveryZoneId());

        User saved = saveUserPort.save(updated);
        LOG.info("[PHONE_CHANGE] Phone updated for userId={} to phone={}", saved.id(), mask(newPhone));
        return saved;
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
