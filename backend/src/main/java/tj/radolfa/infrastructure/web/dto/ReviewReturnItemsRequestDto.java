package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import tj.radolfa.domain.model.Resellability;

import java.util.List;

public record ReviewReturnItemsRequestDto(
        @NotEmpty List<ItemReviewDto> reviews
) {
    public record ItemReviewDto(
            @NotNull Long orderItemId,
            @NotNull Resellability resellability
    ) {}
}
