package tj.radolfa.application.ports.in.home;

import java.util.List;

public interface GetActiveFeaturedCategoriesUseCase {

    /**
     * Read model joining a curated featured-category entry with live category data
     * (name, slug) for the storefront homepage.
     */
    record FeaturedCategoryView(
            Long id,
            Long categoryId,
            String categorySlug,
            String categoryName,
            String imageUrl,
            String title,
            String subtitle,
            int displayOrder
    ) {}

    List<FeaturedCategoryView> execute();
}
