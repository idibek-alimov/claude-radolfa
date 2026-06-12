package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RelocateRequestDto(
        @NotNull Long fromBinId,
        @NotNull Long toBinId,
        @Positive int quantity
) {}
