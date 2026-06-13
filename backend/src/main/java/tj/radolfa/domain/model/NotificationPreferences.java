package tj.radolfa.domain.model;

/**
 * A customer's notification preferences — three independent toggles.
 *
 * <p>Pure Java — zero framework dependencies. One row per user; users with
 * no saved row yet receive {@link #defaults(Long)}.
 */
public record NotificationPreferences(
        Long userId,
        boolean orderUpdates,
        boolean promotions,
        boolean smsMessages) {

    /** Default preferences for a user who has never saved this section. */
    public static NotificationPreferences defaults(Long userId) {
        return new NotificationPreferences(userId, true, true, false);
    }
}
