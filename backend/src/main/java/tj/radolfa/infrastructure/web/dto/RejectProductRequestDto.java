package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectProductRequestDto(
        @NotBlank @Size(max = 1000) String rejectionReason) {}
