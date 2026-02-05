package tj.radolfa.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP storage and generation.
 *
 * <p>This implementation uses an in-memory store suitable for development
 * and single-instance deployments. For production multi-instance deployments,
 * consider switching to Redis-backed storage.
 *
 * <p>In DEV mode (default), OTPs are logged to console for testing.
 * In PROD mode, the actual SMS sending would be integrated here.
 */
@Component
public class OtpStore {

    private static final Logger LOG = LoggerFactory.getLogger(OtpStore.class);

    private final OtpProperties properties;
    private final SecureRandom random = new SecureRandom();

    /**
     * In-memory store: phone -> (otp, expiryInstant)
     */
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    public OtpStore(OtpProperties properties) {
        this.properties = properties;
    }

    /**
     * Generates and stores a new OTP for the given phone number.
     * Any existing OTP for this phone is overwritten.
     *
     * @param phone the phone number to generate OTP for
     * @return the generated OTP code
     */
    public String generateOtp(String phone) {
        String otp = generateRandomOtp();
        Instant expiry = Instant.now().plusSeconds(properties.expirationSeconds());
        store.put(phone, new OtpEntry(otp, expiry));

        // DEV: Log OTP to console (production would send SMS)
        LOG.info("[OTP-DEV] Generated OTP for phone={}: {}", phone, otp);

        return otp;
    }

    /**
     * Verifies an OTP for the given phone number.
     * If valid, the OTP is consumed (removed from store).
     *
     * @param phone the phone number
     * @param otp   the OTP code to verify
     * @return true if OTP is valid and not expired, false otherwise
     */
    public boolean verifyOtp(String phone, String otp) {
        return Optional.ofNullable(store.get(phone))
                .filter(entry -> entry.otp().equals(otp))
                .filter(entry -> Instant.now().isBefore(entry.expiry()))
                .map(entry -> {
                    store.remove(phone);  // consume OTP
                    LOG.debug("[OTP] Successfully verified OTP for phone={}", phone);
                    return true;
                })
                .orElseGet(() -> {
                    LOG.debug("[OTP] Verification failed for phone={}", phone);
                    return false;
                });
    }

    /**
     * Generates a random numeric OTP of the configured length.
     */
    private String generateRandomOtp() {
        int maxValue = (int) Math.pow(10, properties.length());
        int otpNumber = random.nextInt(maxValue);
        return String.format("%0" + properties.length() + "d", otpNumber);
    }

    /**
     * Clears expired OTPs from the store.
     * Can be called periodically to prevent memory growth.
     */
    public void cleanupExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiry()));
    }

    /**
     * Internal record for OTP entry storage.
     */
    private record OtpEntry(String otp, Instant expiry) {}
}
