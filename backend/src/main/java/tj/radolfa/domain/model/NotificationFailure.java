package tj.radolfa.domain.model;

import java.time.Instant;

public record NotificationFailure(
        Long id,
        String notificationType,
        Long userId,
        Long referenceId,
        String errorMessage,
        Instant failedAt,
        boolean alertSent
) {}
