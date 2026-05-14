package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSkuSizeLabelRequestDto(
        @NotBlank(message = "sizeLabel must not be blank")
        String sizeLabel
) {}
