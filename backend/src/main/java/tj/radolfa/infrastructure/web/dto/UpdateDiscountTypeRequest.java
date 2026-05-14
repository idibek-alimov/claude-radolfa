package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import tj.radolfa.domain.model.StackingPolicy;

public record UpdateDiscountTypeRequest(
        @NotBlank String name,
        @Positive int rank,
        StackingPolicy stackingPolicy
) {}
