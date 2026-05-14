package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

public record AddVariantRequestDto(

        @NotNull(message = "colorId is required")
        Long colorId

) {}
