package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PutawayRequestDto(
        @NotNull Long binId,
        @Positive int quantity
) {}
