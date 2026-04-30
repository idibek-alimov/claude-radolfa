package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadReviewTraitPort;
import tj.radolfa.application.ports.out.SaveReviewTraitPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.domain.model.ReviewTraitInputType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DeleteReviewTraitServiceTest {

    private FakeLoadReviewTraitPort fakeLoad;
    private FakeSaveReviewTraitPort fakeSave;
    private DeleteReviewTraitService service;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadReviewTraitPort();
        fakeSave = new FakeSaveReviewTraitPort();
        service  = new DeleteReviewTraitService(fakeLoad, fakeSave);
    }

    @Test
    @DisplayName("existing trait — deleteById is called")
    void execute_existingTrait_deleted() {
        fakeLoad.store(new ReviewTrait(1L, "comfort_level", "label", ReviewTraitInputType.SLIDER, null, null));

        service.execute(1L);

        assertTrue(fakeSave.deletedIds.contains(1L));
    }

    @Test
    @DisplayName("missing trait — throws ResourceNotFoundException")
    void execute_missingTrait_throws() {
        assertThrows(ResourceNotFoundException.class, () -> service.execute(999L));
        assertTrue(fakeSave.deletedIds.isEmpty());
    }

    // =========================================================
    //  Fakes
    // =========================================================

    static class FakeLoadReviewTraitPort implements LoadReviewTraitPort {
        private final Map<Long, ReviewTrait>   byId  = new HashMap<>();
        private final Map<String, ReviewTrait> byKey = new HashMap<>();

        void store(ReviewTrait trait) {
            byId.put(trait.getId(), trait);
            byKey.put(trait.getKey(), trait);
        }

        @Override public Optional<ReviewTrait> findById(Long id)    { return Optional.ofNullable(byId.get(id)); }
        @Override public Optional<ReviewTrait> findByKey(String key) { return Optional.ofNullable(byKey.get(key)); }
        @Override public List<ReviewTrait>     findAll()             { return List.copyOf(byId.values()); }
        @Override public List<ReviewTrait>     findAllByIds(Collection<Long> ids) {
            return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        }
        @Override public boolean existsByKey(String key) { return byKey.containsKey(key); }
    }

    static class FakeSaveReviewTraitPort implements SaveReviewTraitPort {
        final List<Long> deletedIds = new ArrayList<>();

        @Override
        public ReviewTrait save(ReviewTrait trait) { return trait; }

        @Override
        public void deleteById(Long id) { deletedIds.add(id); }
    }
}
