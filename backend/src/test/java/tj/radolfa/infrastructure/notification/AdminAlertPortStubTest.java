package tj.radolfa.infrastructure.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.SaveNotificationFailurePort;
import tj.radolfa.domain.model.NotificationFailure;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminAlertPortStubTest {

    static class CapturingSaveNotificationFailurePort implements SaveNotificationFailurePort {
        final List<NotificationFailure> saved = new ArrayList<>();

        @Override
        public NotificationFailure save(NotificationFailure f) {
            saved.add(f);
            return f;
        }
    }

    @Test
    @DisplayName("Alert logs and persists a NotificationFailure row with alertSent=true")
    void alertPersistsRow() {
        var save = new CapturingSaveNotificationFailurePort();
        new AdminAlertPortStub(save)
                .sendNotificationFailureAlert("DELIVERY_CODE", 10L, 99L, "boom");

        assertEquals(1, save.saved.size());
        NotificationFailure row = save.saved.get(0);
        assertEquals("DELIVERY_CODE", row.notificationType());
        assertEquals(10L,             row.userId());
        assertEquals(99L,             row.referenceId());
        assertEquals("boom",          row.errorMessage());
        assertTrue(row.alertSent());
        assertNotNull(row.failedAt());
        assertNull(row.id());   // no DB assigned — persisted via port
    }
}
