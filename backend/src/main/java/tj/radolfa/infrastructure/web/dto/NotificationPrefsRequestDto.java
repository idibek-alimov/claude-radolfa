package tj.radolfa.infrastructure.web.dto;

/**
 * Request body for updating notification preferences. The owner id is never accepted
 * here — ownership comes from the security principal.
 */
public record NotificationPrefsRequestDto(
        boolean orderUpdates,
        boolean promotions,
        boolean smsMessages) {
}
