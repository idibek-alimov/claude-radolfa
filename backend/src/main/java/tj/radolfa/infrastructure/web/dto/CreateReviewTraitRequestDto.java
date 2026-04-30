package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tj.radolfa.domain.model.ReviewTraitInputType;

public record CreateReviewTraitRequestDto(

        @NotBlank(message = "key is required")
        @Pattern(regexp = "^[a-z][a-z0-9_]*$",
                 message = "key must start with a lowercase letter and contain only lowercase letters, digits, and underscores")
        @Size(max = 64, message = "key must not exceed 64 characters")
        String key,

        @NotBlank(message = "labelI18n is required")
        @Size(max = 255, message = "labelI18n must not exceed 255 characters")
        String labelI18n,

        @NotNull(message = "inputType is required")
        ReviewTraitInputType inputType

) {}
