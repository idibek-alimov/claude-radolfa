package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequestDto(
        @Min(0) int quantity) {
}
