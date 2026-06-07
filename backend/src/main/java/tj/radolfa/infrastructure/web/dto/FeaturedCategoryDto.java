package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.FeaturedCategory;

public record FeaturedCategoryDto(
        Long id,
        Long categoryId,
        String imageUrl,
        String title,
        String subtitle,
        int displayOrder,
        boolean active
) {
    public static FeaturedCategoryDto from(FeaturedCategory featuredCategory) {
        return new FeaturedCategoryDto(
                featuredCategory.id(),
                featuredCategory.categoryId(),
                featuredCategory.imageUrl(),
                featuredCategory.title(),
                featuredCategory.subtitle(),
                featuredCategory.displayOrder(),
                featuredCategory.active());
    }
}
