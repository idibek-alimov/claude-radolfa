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

class UpdateReviewTraitServiceTest {

    private FakeLoadReviewTraitPort fakeLoad;
    private FakeSaveReviewTraitPort fakeSave;
    private UpdateReviewTraitService service;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadReviewTraitPort();
        fakeSave = new FakeSaveReviewTraitPort();
        service  = new UpdateReviewTraitService(fakeLoad, fakeSave);
    }

    @Test
    @DisplayName("happy path — updates label and persists")
    void execute_updatesLabel() {
        fakeLoad.store(new ReviewTrait(1L, "comfort_level", "old.label", ReviewTraitInputType.SLIDER, null, null));

        service.execute(1L, "new.label", ReviewTraitInputType.SLIDER);

        ReviewTrait saved = fakeSave.lastSaved;
        assertNotNull(saved);
        assertEquals("comfort_level", saved.getKey());
        assertEquals("new.label", saved.getLabelI18n());
        assertEquals(ReviewTraitInputType.SLIDER, saved.getInputType());
    }

    @Test
    @DisplayName("happy path — updates inputType")
    void execute_updatesInputType() {
        fakeLoad.store(new ReviewTrait(1L, "fit_type", "reviews.traits.fitType", ReviewTraitInputType.SLIDER, null, null));

        service.execute(1L, "reviews.traits.fitType", ReviewTraitInputType.RADIO);

        assertEquals(ReviewTraitInputType.RADIO, fakeSave.lastSaved.getInputType());
    }

    @Test
    @DisplayName("missing trait ID — throws ResourceNotFoundException")
    void execute_missingId_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.execute(999L, "any.label", ReviewTraitInputType.SLIDER));
    }

    @Test
    @DisplayName("blank labelI18n — throws IllegalArgumentException (domain validation)")
    void execute_blankLabel_throws() {
        fakeLoad.store(new ReviewTrait(1L, "comfort_level", "label", ReviewTraitInputType.SLIDER, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(1L, "  ", ReviewTraitInputType.SLIDER));
    }

    @Test
    @DisplayName("null inputType — throws (domain validation)")
    void execute_nullInputType_throws() {
        fakeLoad.store(new ReviewTrait(1L, "comfort_level", "label", ReviewTraitInputType.SLIDER, null, null));

        assertThrows(Exception.class,
                () -> service.execute(1L, "label", null));
    }

    @Test
    @DisplayName("key remains immutable after update")
    void execute_keyIsImmutable() {
        fakeLoad.store(new ReviewTrait(1L, "comfort_level", "old.label", ReviewTraitInputType.SLIDER, null, null));

        service.execute(1L, "new.label", ReviewTraitInputType.RADIO);

        assertEquals("comfort_level", fakeSave.lastSaved.getKey());
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

        @Override public Optional<ReviewTrait> findById(Long id)     { return Optional.ofNullable(byId.get(id)); }
        @Override public Optional<ReviewTrait> findByKey(String key)  { return Optional.ofNullable(byKey.get(key)); }
        @Override public List<ReviewTrait>     findAll()              { return List.copyOf(byId.values()); }
        @Override public List<ReviewTrait>     findAllByIds(Collection<Long> ids) {
            return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        }
        @Override public boolean existsByKey(String key) { return byKey.containsKey(key); }
        @Override public List<ReviewTrait> findByVariantId(Long listingVariantId) { return List.of(); }
    }

    static class FakeSaveReviewTraitPort implements SaveReviewTraitPort {
        ReviewTrait lastSaved;

        @Override
        public ReviewTrait save(ReviewTrait trait) {
            lastSaved = trait;
            return trait;
        }

        @Override
        public void deleteById(Long id) {}
    }
}
