package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.NotificationPreferences;

public record NotificationPrefsDto(
        boolean orderUpdates,
        boolean promotions,
        boolean smsMessages) {

    public static NotificationPrefsDto from(NotificationPreferences prefs) {
        return new NotificationPrefsDto(prefs.orderUpdates(), prefs.promotions(), prefs.smsMessages());
    }
}
