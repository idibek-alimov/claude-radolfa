package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.ports.in.home.GetActiveFeaturedCategoriesUseCase.FeaturedCategoryView;

import java.math.BigDecimal;

public record FeaturedCategoryPublicDto(
        Long id,
        Long categoryId,
        String categorySlug,
        String categoryName,
        Long productCount,
        BigDecimal minPrice,
        String imageUrl,
        String title,
        String subtitle,
        int displayOrder
) {
    public static FeaturedCategoryPublicDto from(FeaturedCategoryView view) {
        return new FeaturedCategoryPublicDto(
                view.id(),
                view.categoryId(),
                view.categorySlug(),
                view.categoryName(),
                view.productCount(),
                view.minPrice(),
                view.imageUrl(),
                view.title(),
                view.subtitle(),
                view.displayOrder());
    }
}
