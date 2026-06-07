package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tj.radolfa.application.ports.in.home.ManageFeaturedCategoryUseCase;

public record FeaturedCategoryRequestDto(
        @NotNull Long categoryId,
        String imageUrl,
        @Size(max = 160) String title,
        @Size(max = 255) String subtitle,
        @Min(0) int displayOrder,
        boolean active
) {
    public ManageFeaturedCategoryUseCase.Command toCommand() {
        return new ManageFeaturedCategoryUseCase.Command(
                categoryId, imageUrl, title, subtitle, displayOrder, active);
    }
}
