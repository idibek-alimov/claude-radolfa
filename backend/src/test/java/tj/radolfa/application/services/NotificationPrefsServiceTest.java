package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.notification.UpdateNotificationPrefsUseCase;
import tj.radolfa.application.ports.out.LoadNotificationPrefsPort;
import tj.radolfa.application.ports.out.SaveNotificationPrefsPort;
import tj.radolfa.domain.model.NotificationPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPrefsServiceTest {

    private FakeNotificationPrefsPort fakeNotificationPrefsPort;
    private NotificationPrefsService  service;

    @BeforeEach
    void setUp() {
        fakeNotificationPrefsPort = new FakeNotificationPrefsPort();
        service = new NotificationPrefsService(fakeNotificationPrefsPort, fakeNotificationPrefsPort);
    }

    @Test
    @DisplayName("First read returns defaults when no row exists")
    void firstRead_returnsDefaults() {
        NotificationPreferences prefs = service.execute(1L);

        assertEquals(1L, prefs.userId());
        assertTrue(prefs.orderUpdates());
        assertTrue(prefs.promotions());
        assertFalse(prefs.smsMessages());
    }

    @Test
    @DisplayName("Update persists the new preferences")
    void update_persists() {
        service.execute(new UpdateNotificationPrefsUseCase.Command(1L, false, true, true));

        NotificationPreferences stored = fakeNotificationPrefsPort.findByUserId(1L).orElseThrow();

        assertFalse(stored.orderUpdates());
        assertTrue(stored.promotions());
        assertTrue(stored.smsMessages());
    }

    @Test
    @DisplayName("Second read reflects the updated preferences")
    void secondRead_reflectsUpdate() {
        service.execute(new UpdateNotificationPrefsUseCase.Command(1L, false, false, true));

        NotificationPreferences prefs = service.execute(1L);

        assertFalse(prefs.orderUpdates());
        assertFalse(prefs.promotions());
        assertTrue(prefs.smsMessages());
    }

    @Test
    @DisplayName("Update for a user with no prior row creates one")
    void update_withNoPriorRow_createsRow() {
        assertTrue(fakeNotificationPrefsPort.findByUserId(2L).isEmpty());

        service.execute(new UpdateNotificationPrefsUseCase.Command(2L, true, false, false));

        assertTrue(fakeNotificationPrefsPort.findByUserId(2L).isPresent());
    }

    // =========================================================
    //  In-memory fake
    // =========================================================

    static class FakeNotificationPrefsPort implements LoadNotificationPrefsPort, SaveNotificationPrefsPort {

        private final Map<Long, NotificationPreferences> byUserId = new HashMap<>();

        @Override
        public Optional<NotificationPreferences> findByUserId(Long userId) {
            return Optional.ofNullable(byUserId.get(userId));
        }

        @Override
        public NotificationPreferences save(NotificationPreferences prefs) {
            byUserId.put(prefs.userId(), prefs);
            return prefs;
        }
    }
}
