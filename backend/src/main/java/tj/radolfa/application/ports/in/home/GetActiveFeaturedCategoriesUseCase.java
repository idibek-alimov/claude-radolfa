package tj.radolfa.application.ports.in.home;

import java.math.BigDecimal;
import java.util.List;

public interface GetActiveFeaturedCategoriesUseCase {

    /**
     * Read model joining a curated featured-category entry with live category data
     * (name, slug, product count, minimum price) for the storefront homepage.
     */
    record FeaturedCategoryView(
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
    ) {}

    List<FeaturedCategoryView> execute();
}
