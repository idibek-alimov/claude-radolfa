package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.GetCategoryUseCase;
import tj.radolfa.application.ports.in.home.ManageFeaturedCategoryUseCase.Command;
import tj.radolfa.application.ports.out.LoadFeaturedCategoryPort;
import tj.radolfa.application.ports.out.SaveFeaturedCategoryPort;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.FeaturedCategory;
import tj.radolfa.domain.model.PageResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ManageFeaturedCategoryServiceTest {

    private FakeLoadFeaturedCategoryPort fakeLoad;
    private FakeSaveFeaturedCategoryPort fakeSave;
    private FakeGetCategoryUseCase fakeCategories;
    private ManageFeaturedCategoryService service;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadFeaturedCategoryPort();
        fakeSave = new FakeSaveFeaturedCategoryPort(fakeLoad);
        fakeCategories = new FakeGetCategoryUseCase();
        fakeCategories.put(category(1L, "Phones", "phones"));
        fakeCategories.put(category(2L, "Laptops", "laptops"));
        service = new ManageFeaturedCategoryService(fakeLoad, fakeSave, fakeCategories);
    }

    // ----------------------------------------------------------------
    //  create
    // ----------------------------------------------------------------

    @Test
    void createWithValidCategoryPersistsAndAssignsId() {
        FeaturedCategory result = service.create(command(1L, 0));

        assertThat(result.id()).isNotNull();
        assertThat(result.categoryId()).isEqualTo(1L);
        assertThat(fakeLoad.findById(result.id())).isPresent();
    }

    @Test
    void createWithUnknownCategoryThrowsAndSavesNothing() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.create(command(999L, 0)));

        assertThat(fakeLoad.findActiveOrdered()).isEmpty();
    }

    // ----------------------------------------------------------------
    //  update
    // ----------------------------------------------------------------

    @Test
    void updateMutatesStoredEntry() {
        FeaturedCategory existing = service.create(command(1L, 0));

        FeaturedCategory updated = service.update(existing.id(), commandWithTitle(2L, 3, "New title"));

        assertThat(updated.id()).isEqualTo(existing.id());
        assertThat(updated.categoryId()).isEqualTo(2L);
        assertThat(updated.displayOrder()).isEqualTo(3);
        assertThat(updated.title()).isEqualTo("New title");
    }

    @Test
    void updateUnknownIdThrows() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.update(999L, command(1L, 0)));
    }

    @Test
    void updateWithUnknownCategoryThrows() {
        FeaturedCategory existing = service.create(command(1L, 0));

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(existing.id(), command(999L, 0)));
    }

    // ----------------------------------------------------------------
    //  delete
    // ----------------------------------------------------------------

    @Test
    void deleteDelegatesToSavePort() {
        FeaturedCategory entry = service.create(command(1L, 0));

        service.delete(entry.id());

        assertThat(fakeLoad.findById(entry.id())).isEmpty();
    }

    // ----------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------

    private Command command(Long categoryId, int displayOrder) {
        return new Command(categoryId, null, null, null, displayOrder, true);
    }

    private Command commandWithTitle(Long categoryId, int displayOrder, String title) {
        return new Command(categoryId, null, title, null, displayOrder, true);
    }

    private CategoryView category(Long id, String name, String slug) {
        return new CategoryView(id, name, slug, null, List.of());
    }

    // ----------------------------------------------------------------
    //  Fakes
    // ----------------------------------------------------------------

    static class FakeLoadFeaturedCategoryPort implements LoadFeaturedCategoryPort {
        private final List<FeaturedCategory> store = new ArrayList<>();

        @Override public Optional<FeaturedCategory> findById(Long id) {
            return store.stream().filter(f -> f.id().equals(id)).findFirst();
        }
        @Override public List<FeaturedCategory> findActiveOrdered() {
            return store.stream().filter(FeaturedCategory::active)
                    .sorted((a, b) -> Integer.compare(a.displayOrder(), b.displayOrder())).toList();
        }
        @Override public PageResult<FeaturedCategory> findAllPaged(int page, int size, String search, String sort) {
            return new PageResult<>(List.copyOf(store), store.size(), page, size, true);
        }

        void removeById(Long id) { store.removeIf(f -> f.id().equals(id)); }
        void put(FeaturedCategory f) { store.removeIf(x -> x.id().equals(f.id())); store.add(f); }
    }

    static class FakeSaveFeaturedCategoryPort implements SaveFeaturedCategoryPort {
        private final FakeLoadFeaturedCategoryPort load;
        private final AtomicLong seq = new AtomicLong(1);

        FakeSaveFeaturedCategoryPort(FakeLoadFeaturedCategoryPort load) { this.load = load; }

        @Override
        public FeaturedCategory save(FeaturedCategory featuredCategory) {
            Long id = featuredCategory.id() != null ? featuredCategory.id() : seq.getAndIncrement();
            FeaturedCategory saved = new FeaturedCategory(id, featuredCategory.categoryId(),
                    featuredCategory.imageUrl(), featuredCategory.title(), featuredCategory.subtitle(),
                    featuredCategory.displayOrder(), featuredCategory.active());
            load.put(saved);
            return saved;
        }

        @Override
        public void delete(Long id) { load.removeById(id); }
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
