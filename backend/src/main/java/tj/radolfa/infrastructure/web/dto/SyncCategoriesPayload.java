package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SyncCategoriesPayload(
        @NotEmpty @Valid List<CategoryPayload> categories
) {
    public record CategoryPayload(
            @NotBlank String name,
            String parentName  // null = root
    ) {}
}
