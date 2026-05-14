package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tj.radolfa.domain.model.ReviewTraitInputType;

public record UpdateReviewTraitRequestDto(

        @NotBlank(message = "labelI18n is required")
        @Size(max = 255, message = "labelI18n must not exceed 255 characters")
        String labelI18n,

        @NotNull(message = "inputType is required")
        ReviewTraitInputType inputType

) {}
