package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateColorDto(

        @NotBlank
        @Pattern(
                regexp = "^[a-z0-9_]+$",
                message = "colorKey must contain only lowercase letters, digits, and underscores"
        )
        String colorKey,

        @NotBlank
        String displayName,

        @NotBlank
        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "hexCode must be in #RRGGBB format"
        )
        String hexCode
) {}
