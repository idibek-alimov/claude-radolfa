package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.FeaturedCategory;

public interface SaveFeaturedCategoryPort {
    FeaturedCategory save(FeaturedCategory featuredCategory);
    void delete(Long id);
}
