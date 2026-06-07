package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.GetCategoryUseCase;
import tj.radolfa.application.ports.in.home.GetActiveFeaturedCategoriesUseCase;
import tj.radolfa.application.ports.out.LoadFeaturedCategoryPort;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.domain.model.FeaturedCategory;

import java.util.List;
import java.util.Objects;

@Service
public class GetActiveFeaturedCategoriesService implements GetActiveFeaturedCategoriesUseCase {

    private final LoadFeaturedCategoryPort loadFeaturedCategoryPort;
    private final GetCategoryUseCase getCategoryUseCase;

    public GetActiveFeaturedCategoriesService(LoadFeaturedCategoryPort loadFeaturedCategoryPort,
                                              GetCategoryUseCase getCategoryUseCase) {
        this.loadFeaturedCategoryPort = loadFeaturedCategoryPort;
        this.getCategoryUseCase = getCategoryUseCase;
    }

    @Override
    public List<FeaturedCategoryView> execute() {
        return loadFeaturedCategoryPort.findActiveOrdered().stream()
                .map(this::toView)
                .filter(Objects::nonNull)
                .toList();
    }

    private FeaturedCategoryView toView(FeaturedCategory entry) {
        CategoryView category = getCategoryUseCase.findById(entry.categoryId()).orElse(null);
        if (category == null) {
            return null; // category was deleted/unavailable — skip the orphaned entry
        }
        return new FeaturedCategoryView(
                entry.id(),
                entry.categoryId(),
                category.slug(),
                category.name(),
                entry.imageUrl(),
                entry.title(),
                entry.subtitle(),
                entry.displayOrder());
    }
}
