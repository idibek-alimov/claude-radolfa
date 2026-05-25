package tj.radolfa.application.ports.out;

public interface AdminAlertPort {

    /**
     * Fires when all retries for a customer notification have been exhausted.
     * Current implementation: log at ERROR and persist a NotificationFailure row.
     * Future (mobile app): push notification to admin.
     */
    void sendNotificationFailureAlert(String notificationType,
                                       Long userId,
                                       Long referenceId,
                                       String errorMessage);
}
