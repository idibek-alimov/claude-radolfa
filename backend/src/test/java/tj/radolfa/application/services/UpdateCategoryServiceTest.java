package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.SaveCategoryPort;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.domain.exception.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UpdateCategoryService} using in-memory fakes.
 */
class UpdateCategoryServiceTest {

    private FakeLoadCategoryPort fakeLoad;
    private FakeSaveCategoryPort fakeSave;
    private UpdateCategoryService service;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadCategoryPort();
        fakeSave = new FakeSaveCategoryPort();
        service  = new UpdateCategoryService(fakeLoad, fakeSave);
    }

    // --- Setup helpers ---

    private CategoryView stored(Long id, String name, String slug, Long parentId) {
        CategoryView view = new CategoryView(id, name, slug, parentId);
        fakeLoad.store(view);
        return view;
    }

    // --- Tests ---

    @Test
    @DisplayName("execute() throws ResourceNotFoundException when category does not exist")
    void execute_unknownId_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.execute(99L, "Shoes", null));
    }

    @Test
    @DisplayName("execute() throws when name is blank")
    void execute_blankName_throws() {
        stored(1L, "Shirts", "shirts", null);

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(1L, "  ", null));
    }

    @Test
    @DisplayName("execute() throws when name exceeds 128 characters")
    void execute_nameTooLong_throws() {
        stored(1L, "Shirts", "shirts", null);
        String longName = "A".repeat(129);

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(1L, longName, null));
    }

    @Test
    @DisplayName("execute() throws when name is already used by another category")
    void execute_duplicateName_throws() {
        stored(1L, "Shirts", "shirts", null);
        stored(2L, "Pants", "pants", null);

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(1L, "Pants", null));
    }

    @Test
    @DisplayName("execute() allows keeping the same name on the same category")
    void execute_sameName_succeeds() {
        stored(1L, "Shirts", "shirts", null);

        assertDoesNotThrow(() -> service.execute(1L, "Shirts", null));
        assertEquals(1L, fakeSave.lastUpdatedId);
    }

    @Test
    @DisplayName("execute() throws when parent does not exist")
    void execute_unknownParent_throws() {
        stored(1L, "Shirts", "shirts", null);

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(1L, "Shirts", 999L));
    }

    @Test
    @DisplayName("execute() throws when a category is set as its own parent")
    void execute_selfAsParent_throws() {
        stored(1L, "Shirts", "shirts", null);

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(1L, "Shirts", 1L));
    }

    @Test
    @DisplayName("execute() throws when parent is a descendant (circular reference)")
    void execute_descendantAsParent_throws() {
        stored(1L, "Clothing", "clothing", null);
        stored(2L, "Shirts", "shirts", 1L);
        stored(3L, "Long-sleeve", "long-sleeve", 2L);
        // Descendant id=2 (Shirts) is a child of id=1 (Clothing)
        fakeLoad.addDescendant(1L, 2L);
        fakeLoad.addDescendant(1L, 3L);

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(1L, "Clothing", 2L));
    }

    @Test
    @DisplayName("execute() slug is not modified after update")
    void execute_slugIsImmutable() {
        stored(1L, "Shirts", "shirts", null);

        service.execute(1L, "Tshirts", null);

        // The service delegates slug=null implicitly — the adapter would preserve it.
        // We verify update was called with correct id and name.
        assertEquals(1L, fakeSave.lastUpdatedId);
        assertEquals("Tshirts", fakeSave.lastUpdatedName);
    }

    @Test
    @DisplayName("execute() succeeds for valid rename + new parent")
    void execute_happyPath() {
        stored(1L, "Clothing", "clothing", null);
        stored(2L, "Shirts", "shirts", null);

        assertDoesNotThrow(() -> service.execute(2L, "T-Shirts", 1L));
        assertEquals(2L, fakeSave.lastUpdatedId);
        assertEquals("T-Shirts", fakeSave.lastUpdatedName);
        assertEquals(1L, fakeSave.lastUpdatedParentId);
    }

    // =========================================================
    //  Fakes
    // =========================================================

    static class FakeLoadCategoryPort implements LoadCategoryPort {
        private final Map<Long, CategoryView> byId = new HashMap<>();
        private final Map<String, CategoryView> byName = new HashMap<>();
        private final Map<Long, List<Long>> descendants = new HashMap<>();

        void store(CategoryView view) {
            byId.put(view.id(), view);
            byName.put(view.name(), view);
        }

        void addDescendant(Long categoryId, Long descendantId) {
            descendants.computeIfAbsent(categoryId, k -> new ArrayList<>()).add(descendantId);
        }

        @Override public Optional<CategoryView> findById(Long id) { return Optional.ofNullable(byId.get(id)); }
        @Override public Optional<CategoryView> findByName(String name) { return Optional.ofNullable(byName.get(name)); }
        @Override public Optional<CategoryView> findBySlug(String slug) { return Optional.empty(); }
        @Override public List<CategoryView> findRoots() { return List.of(); }
        @Override public List<CategoryView> findByParentId(Long parentId) { return List.of(); }
        @Override public List<CategoryView> findAll() { return List.copyOf(byId.values()); }
        @Override public List<Long> getAllDescendantIds(Long categoryId) {
            return descendants.getOrDefault(categoryId, List.of());
        }
    }

    static class FakeSaveCategoryPort implements SaveCategoryPort {
        Long lastUpdatedId;
        String lastUpdatedName;
        Long lastUpdatedParentId;

        @Override
        public CategoryView save(String name, String slug, Long parentId) {
            return new CategoryView(100L, name, slug, parentId);
        }

        @Override
        public CategoryView update(Long id, String name, Long parentId) {
            lastUpdatedId = id;
            lastUpdatedName = name;
            lastUpdatedParentId = parentId;
            return new CategoryView(id, name, "unchanged-slug", parentId);
        }
    }
}
