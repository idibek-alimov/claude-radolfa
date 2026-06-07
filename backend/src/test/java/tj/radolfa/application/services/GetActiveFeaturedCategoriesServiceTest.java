package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.GetCategoryUseCase;
import tj.radolfa.application.ports.in.home.GetActiveFeaturedCategoriesUseCase.FeaturedCategoryView;
import tj.radolfa.application.ports.out.LoadFeaturedCategoryPort;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.domain.model.FeaturedCategory;
import tj.radolfa.domain.model.PageResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GetActiveFeaturedCategoriesServiceTest {

    private FakeLoadFeaturedCategoryPort fakeLoad;
    private FakeGetCategoryUseCase fakeCategories;
    private GetActiveFeaturedCategoriesService service;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadFeaturedCategoryPort();
        fakeCategories = new FakeGetCategoryUseCase();
        service = new GetActiveFeaturedCategoriesService(fakeLoad, fakeCategories);
    }

    @Test
    void resolvesEachEntryWithLiveCategoryDataInDisplayOrder() {
        fakeCategories.put(category(1L, "Phones", "phones"));
        fakeCategories.put(category(2L, "Laptops", "laptops"));

        // returned already sorted by displayOrder, mirroring findActiveOrdered()
        fakeLoad.entries.add(entry(10L, 2L, 0, "Laptops Spotlight"));
        fakeLoad.entries.add(entry(11L, 1L, 1, null));

        List<FeaturedCategoryView> result = service.execute();

        assertThat(result).hasSize(2);

        FeaturedCategoryView first = result.get(0);
        assertThat(first.id()).isEqualTo(10L);
        assertThat(first.categoryId()).isEqualTo(2L);
        assertThat(first.categorySlug()).isEqualTo("laptops");
        assertThat(first.categoryName()).isEqualTo("Laptops");
        assertThat(first.title()).isEqualTo("Laptops Spotlight");

        FeaturedCategoryView second = result.get(1);
        assertThat(second.id()).isEqualTo(11L);
        assertThat(second.categoryId()).isEqualTo(1L);
        assertThat(second.categorySlug()).isEqualTo("phones");
    }

    @Test
    void skipsEntryWhoseCategoryIsMissing() {
        fakeCategories.put(category(1L, "Phones", "phones"));
        fakeLoad.entries.add(entry(10L, 1L, 0, null));
        fakeLoad.entries.add(entry(11L, 999L, 1, null)); // category 999 does not exist

        List<FeaturedCategoryView> result = service.execute();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryId()).isEqualTo(1L);
    }

    @Test
    void returnsEmptyListWhenNoActiveEntries() {
        assertThat(service.execute()).isEmpty();
    }

    // ----------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------

    private FeaturedCategory entry(Long id, Long categoryId, int displayOrder, String title) {
        return new FeaturedCategory(id, categoryId, null, title, null, displayOrder, true);
    }

    private CategoryView category(Long id, String name, String slug) {
        return new CategoryView(id, name, slug, null, List.of());
    }

    // ----------------------------------------------------------------
    //  Fakes
    // ----------------------------------------------------------------

    static class FakeLoadFeaturedCategoryPort implements LoadFeaturedCategoryPort {
        final List<FeaturedCategory> entries = new ArrayList<>();

        @Override public Optional<FeaturedCategory> findById(Long id) {
            return entries.stream().filter(f -> f.id().equals(id)).findFirst();
        }
        @Override public List<FeaturedCategory> findActiveOrdered() { return List.copyOf(entries); }
        @Override public PageResult<FeaturedCategory> findAllPaged(int page, int size, String search, String sort) {
            return new PageResult<>(List.copyOf(entries), entries.size(), page, size, true);
        }
    }

    static class FakeGetCategoryUseCase implements GetCategoryUseCase {
        private final List<CategoryView> store = new ArrayList<>();

        void put(CategoryView view) { store.removeIf(c -> c.id().equals(view.id())); store.add(view); }

        @Override public List<CategoryView> findAll() { return List.copyOf(store); }
        @Override public Optional<CategoryView> findBySlug(String slug) {
            return store.stream().filter(c -> c.slug().equals(slug)).findFirst();
        }
        @Override public List<Long> getDescendantIds(Long categoryId) { return List.of(categoryId); }
        @Override public Optional<CategoryView> findById(Long id) {
            return store.stream().filter(c -> c.id().equals(id)).findFirst();
        }
    }
}
