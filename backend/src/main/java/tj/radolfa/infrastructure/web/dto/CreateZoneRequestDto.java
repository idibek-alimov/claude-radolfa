package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateZoneRequestDto(
        @NotBlank @Size(max = 20) String code,
        @Size(max = 100)          String label
) {}
