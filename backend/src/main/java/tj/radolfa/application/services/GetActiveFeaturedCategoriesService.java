package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.GetCategoryUseCase;
import tj.radolfa.application.ports.in.home.GetActiveFeaturedCategoriesUseCase;
import tj.radolfa.application.ports.out.LoadFeaturedCategoryPort;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.domain.model.FeaturedCategory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        List<FeaturedCategory> activeOrdered = loadFeaturedCategoryPort.findActiveOrdered();
        if (activeOrdered.isEmpty()) {
            return List.of();
        }

        // findById leaves productCount/minPrice null — findAll() is the only source that
        // populates those rollups, so resolve every entry against a single indexed snapshot.
        Map<Long, CategoryView> categoriesById = getCategoryUseCase.findAll().stream()
                .collect(Collectors.toMap(CategoryView::id, Function.identity()));

        return activeOrdered.stream()
                .map(entry -> toView(entry, categoriesById.get(entry.categoryId())))
                .filter(Objects::nonNull)
                .toList();
    }

    private FeaturedCategoryView toView(FeaturedCategory entry, CategoryView category) {
        if (category == null) {
            return null; // category was deleted/unavailable — skip the orphaned entry
        }
        return new FeaturedCategoryView(
                entry.id(),
                entry.categoryId(),
                category.slug(),
                category.name(),
                category.productCount(),
                category.minPrice(),
                entry.imageUrl(),
                entry.title(),
                entry.subtitle(),
                entry.displayOrder());
    }
}
