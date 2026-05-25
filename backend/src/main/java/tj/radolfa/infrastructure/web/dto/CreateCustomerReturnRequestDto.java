package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tj.radolfa.domain.model.ReturnReason;

import java.util.List;

public record CreateCustomerReturnRequestDto(
        @NotNull Long orderId,
        @Size(max = 500) String notes,
        @NotEmpty @Valid List<ReturnItemRequestDto> items) {

    public record ReturnItemRequestDto(
            @NotNull Long orderItemId,
            @Min(1) int quantity,
            @NotNull ReturnReason reason,
            @Size(max = 500) String notes) {}
}
