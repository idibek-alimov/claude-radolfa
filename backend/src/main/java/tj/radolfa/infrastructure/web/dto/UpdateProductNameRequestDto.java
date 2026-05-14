package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProductNameRequestDto(
        @NotBlank(message = "name must not be blank")
        String name
) {}
