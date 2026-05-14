package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderImagesRequestDto(

        @NotNull(message = "imageIds is required")
        @NotEmpty(message = "imageIds must not be empty")
        List<@NotNull Long> imageIds

) {}
