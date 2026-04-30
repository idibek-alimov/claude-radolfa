package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateCategoryRequestDto(

        @NotBlank(message = "Category name is required")
        @Size(max = 128, message = "Category name must not exceed 128 characters")
        String name,

        /** New parent category ID, or null to promote to root. */
        Long parentId,

        /** IDs of review traits to link; null leaves existing links untouched. */
        Set<@Positive Long> traitIds

) {}
