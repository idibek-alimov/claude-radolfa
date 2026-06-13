package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.user.ConfirmPhoneChangeUseCase;
import tj.radolfa.application.ports.in.user.RequestPhoneChangeUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.OtpPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class PhoneChangeServiceTest {

    private static final Long CALLER_ID = 1L;
    private static final String CALLER_PHONE = "+992900000001";
    private static final String NEW_PHONE = "+992900000002";
    private static final String OTHER_PHONE = "+992900000003";

    private FakeUserPort fakeUserPort;
    private FakeOtpPort fakeOtpPort;
    private FakeNotificationPort fakeNotificationPort;
    private PhoneChangeService service;

    @BeforeEach
    void setUp() {
        fakeUserPort = new FakeUserPort();
        fakeUserPort.addUser(CALLER_ID, CALLER_PHONE);
        fakeUserPort.addUser(2L, OTHER_PHONE);

        fakeOtpPort = new FakeOtpPort(true);
        fakeNotificationPort = new FakeNotificationPort();

        service = new PhoneChangeService(fakeOtpPort, fakeUserPort, fakeUserPort, fakeNotificationPort);
    }

    @Test
    @DisplayName("Request generates and sends an OTP to the new phone number")
    void request_generatesAndSendsOtp() {
        service.execute(new RequestPhoneChangeUseCase.Command(CALLER_ID, NEW_PHONE));

        assertEquals(NEW_PHONE, fakeOtpPort.lastGeneratedPhone);
        assertEquals(NEW_PHONE, fakeNotificationPort.lastPhone);
        assertEquals(fakeOtpPort.lastCode, fakeNotificationPort.lastCode);
    }

    @Test
    @DisplayName("Request for a number already taken by another user is rejected")
    void request_takenNumber_throwsDuplicate() {
        assertThrows(DuplicateResourceException.class,
                () -> service.execute(new RequestPhoneChangeUseCase.Command(CALLER_ID, OTHER_PHONE)));

        assertNull(fakeOtpPort.lastGeneratedPhone);
        assertNull(fakeNotificationPort.lastPhone);
    }

    @Test
    @DisplayName("Confirm with the right code updates the phone and persists it")
    void confirm_correctCode_updatesPhone() {
        service.execute(new RequestPhoneChangeUseCase.Command(CALLER_ID, NEW_PHONE));

        User updated = service.execute(new ConfirmPhoneChangeUseCase.Command(CALLER_ID, NEW_PHONE, "1234"));

        assertEquals(NEW_PHONE, updated.phone().value());
        assertEquals(NEW_PHONE, fakeUserPort.lastSaved.phone().value());
        assertEquals(CALLER_ID, fakeUserPort.lastSaved.id());
    }

    @Test
    @DisplayName("Confirm with the wrong code is rejected and does not persist")
    void confirm_wrongCode_throwsAndDoesNotSave() {
        fakeOtpPort = new FakeOtpPort(false);
        service = new PhoneChangeService(fakeOtpPort, fakeUserPort, fakeUserPort, fakeNotificationPort);

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new ConfirmPhoneChangeUseCase.Command(CALLER_ID, NEW_PHONE, "0000")));

        assertNull(fakeUserPort.lastSaved);
    }

    @Test
    @DisplayName("Confirm for a number taken by another user is rejected and does not persist")
    void confirm_takenNumber_throwsDuplicateAndDoesNotSave() {
        assertThrows(DuplicateResourceException.class,
                () -> service.execute(new ConfirmPhoneChangeUseCase.Command(CALLER_ID, OTHER_PHONE, "1234")));

        assertNull(fakeUserPort.lastSaved);
    }

    // =========================================================
    //  In-memory fakes
    // =========================================================

    static class FakeUserPort implements LoadUserPort, SaveUserPort {

        private final List<User> users = new ArrayList<>();
        private final AtomicLong idGen = new AtomicLong(1);
        User lastSaved;

        void addUser(Long id, String phone) {
            users.add(new User(id, new PhoneNumber(phone), UserRole.USER, "Test User", null,
                    LoyaltyProfile.empty(), true, idGen.getAndIncrement()));
        }

        @Override
        public Optional<User> loadByPhone(String phone) {
            return users.stream().filter(u -> u.phone().value().equals(phone)).findFirst();
        }

        @Override
        public Optional<User> loadById(Long id) {
            return users.stream().filter(u -> u.id().equals(id)).findFirst();
        }

        @Override
        public List<User> findAllNonPermanent() {
            return List.of();
        }

        @Override
        public List<User> findByRoleAndEnabledTrue(UserRole role) {
            return List.of();
        }

        @Override
        public User save(User user) {
            users.removeIf(u -> u.id().equals(user.id()));
            users.add(user);
            lastSaved = user;
            return user;
        }
    }

    static class FakeOtpPort implements OtpPort {
        private final boolean verifyResult;
        String lastGeneratedPhone;
        String lastCode;

        FakeOtpPort(boolean verifyResult) {
            this.verifyResult = verifyResult;
        }

        @Override
        public String generateOtp(String phone) {
            lastGeneratedPhone = phone;
            lastCode = "1234";
            return lastCode;
        }

        @Override
        public boolean verifyOtp(String phone, String otp) {
            return verifyResult;
        }
    }

    static class FakeNotificationPort implements NotificationPort {
        String lastPhone;
        String lastCode;

        @Override
        public void sendOtpCode(String phone, String code) {
            lastPhone = phone;
            lastCode = code;
        }

        @Override
        public void sendOrderConfirmation(Long userId, Long orderId) {}

        @Override
        public void sendOrderStatusUpdate(Long userId, Long orderId, tj.radolfa.domain.model.OrderStatus newStatus) {}

        @Override
        public void sendReviewApprovedNotification(Long userId, Long reviewId) {}

        @Override
        public void sendReviewReplyNotification(Long userId, Long reviewId) {}

        @Override
        public void sendDeliveryCode(Long userId, Long orderId, String code, java.time.Instant expiresAt) {}

        @Override
        public void sendPickpointExpiryWarning(Long userId, Long orderId, int daysRemaining) {}

        @Override
        public void sendPickpointOrderExpiredCancellation(Long userId, Long orderId) {}
    }
}
