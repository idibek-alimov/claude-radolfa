package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBinRequestDto(
        @NotBlank @Size(max = 20) String code
) {}
