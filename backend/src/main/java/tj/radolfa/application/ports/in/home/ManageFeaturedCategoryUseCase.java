package tj.radolfa.application.ports.in.home;

import tj.radolfa.domain.model.FeaturedCategory;

public interface ManageFeaturedCategoryUseCase {

    record Command(
            Long categoryId,
            String imageUrl,
            String title,
            String subtitle,
            int displayOrder,
            boolean active
    ) {}

    FeaturedCategory create(Command command);
    FeaturedCategory update(Long id, Command command);
    void delete(Long id);
}
