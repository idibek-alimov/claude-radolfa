package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import tj.radolfa.application.ports.out.AdminAlertPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryCodeNotificationServiceTest {

    static final Long   USER_ID    = 10L;
    static final Long   ORDER_ID   = 1L;
    static final String CODE       = "12345678";
    static final Instant EXPIRES   = Instant.now().plusSeconds(3600);

    // ── Fakes ────────────────────────────────────────────────────────────────

    static class FlakyNotificationPort implements NotificationPort {
        int callCount    = 0;
        int failUntilCall = 0;    // throw on calls 1..failUntilCall, succeed thereafter

        record DeliveryCodeCall(Long userId, Long orderId, String code, Instant expiresAt) {}
        DeliveryCodeCall lastCall;

        @Override
        public void sendDeliveryCode(Long uid, Long oid, String code, Instant exp) {
            callCount++;
            if (callCount <= failUntilCall) {
                throw new RuntimeException("SMS provider down (attempt " + callCount + ")");
            }
            lastCall = new DeliveryCodeCall(uid, oid, code, exp);
        }

        @Override public void sendOrderConfirmation(Long u, Long o) {}
        @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) {}
        @Override public void sendReviewApprovedNotification(Long u, Long r) {}
        @Override public void sendReviewReplyNotification(Long u, Long r) {}
        @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
        @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
    }

    static class RecordingAdminAlertPort implements AdminAlertPort {
        record Alert(String type, Long userId, Long refId, String error) {}
        final List<Alert> alerts = new ArrayList<>();

        @Override
        public void sendNotificationFailureAlert(String t, Long u, Long r, String e) {
            alerts.add(new Alert(t, u, r, e));
        }
    }

    static RetryTemplate fastRetry(int maxAttempts) {
        RetryTemplate t = new RetryTemplate();
        t.setRetryPolicy(new SimpleRetryPolicy(maxAttempts));
        t.setBackOffPolicy(new NoBackOffPolicy());
        return t;
    }

    static DeliveryCodeNotificationService service(FlakyNotificationPort notif,
                                                    RecordingAdminAlertPort alert,
                                                    int maxAttempts) {
        return new DeliveryCodeNotificationService(notif, alert, fastRetry(maxAttempts));
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("First call succeeds → SMS sent once, no admin alert")
    void firstCallSucceeds_noAlert() {
        var notif  = new FlakyNotificationPort();
        var alert  = new RecordingAdminAlertPort();
        var svc    = service(notif, alert, 3);

        svc.send(USER_ID, ORDER_ID, CODE, EXPIRES);

        assertEquals(1, notif.callCount);
        assertNotNull(notif.lastCall);
        assertEquals(USER_ID, notif.lastCall.userId());
        assertEquals(ORDER_ID, notif.lastCall.orderId());
        assertTrue(alert.alerts.isEmpty());
    }

    @Test
    @DisplayName("Fails twice then succeeds on 3rd attempt → no admin alert")
    void failsTwiceThenSucceeds_noAlert() {
        var notif  = new FlakyNotificationPort();
        notif.failUntilCall = 2;
        var alert  = new RecordingAdminAlertPort();
        var svc    = service(notif, alert, 3);

        svc.send(USER_ID, ORDER_ID, CODE, EXPIRES);

        assertEquals(3, notif.callCount);
        assertNotNull(notif.lastCall);
        assertTrue(alert.alerts.isEmpty());
    }

    @Test
    @DisplayName("Fails all 3 attempts → admin alert fired with correct metadata")
    void allAttemptsExhausted_adminAlertFired() {
        var notif  = new FlakyNotificationPort();
        notif.failUntilCall = 3;   // always fails
        var alert  = new RecordingAdminAlertPort();
        var svc    = service(notif, alert, 3);

        svc.send(USER_ID, ORDER_ID, CODE, EXPIRES);

        assertEquals(3, notif.callCount);
        assertEquals(1, alert.alerts.size());
        var a = alert.alerts.get(0);
        assertEquals("DELIVERY_CODE", a.type());
        assertEquals(USER_ID,  a.userId());
        assertEquals(ORDER_ID, a.refId());
        assertTrue(a.error().contains("SMS provider down"));
    }

    @Test
    @DisplayName("Exhausted retries do not propagate — caller sees no exception")
    void exhaustedRetries_doNotPropagate() {
        var notif = new FlakyNotificationPort();
        notif.failUntilCall = 10;
        var svc   = service(notif, new RecordingAdminAlertPort(), 3);

        assertDoesNotThrow(() -> svc.send(USER_ID, ORDER_ID, CODE, EXPIRES));
    }
}
