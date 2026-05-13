package tj.radolfa.domain.model;

import java.time.LocalTime;

public record PickpointHours(
        Long id,
        Long pickpointId,
        int dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime
) {}
