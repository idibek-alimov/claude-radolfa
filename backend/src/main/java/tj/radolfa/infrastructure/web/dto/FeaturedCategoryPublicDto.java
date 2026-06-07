package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.ports.in.home.GetActiveFeaturedCategoriesUseCase.FeaturedCategoryView;

public record FeaturedCategoryPublicDto(
        Long id,
        Long categoryId,
        String categorySlug,
        String categoryName,
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
                view.imageUrl(),
                view.title(),
                view.subtitle(),
                view.displayOrder());
    }
}
