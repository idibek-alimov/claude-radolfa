package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadReviewTraitPort;
import tj.radolfa.application.ports.out.SaveReviewTraitPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.domain.model.ReviewTraitInputType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CreateReviewTraitServiceTest {

    private FakeLoadReviewTraitPort fakeLoad;
    private FakeSaveReviewTraitPort fakeSave;
    private CreateReviewTraitService service;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadReviewTraitPort();
        fakeSave = new FakeSaveReviewTraitPort();
        service  = new CreateReviewTraitService(fakeLoad, fakeSave);
    }

    @Test
    @DisplayName("happy path — creates trait and returns assigned ID")
    void execute_happyPath_returnId() {
        Long id = service.execute("comfort_level", "reviews.traits.comfortLevel", ReviewTraitInputType.SLIDER);

        assertNotNull(id);
        assertEquals(1, fakeSave.savedTraits.size());
        ReviewTrait saved = fakeSave.savedTraits.get(0);
        assertEquals("comfort_level", saved.getKey());
        assertEquals("reviews.traits.comfortLevel", saved.getLabelI18n());
        assertEquals(ReviewTraitInputType.SLIDER, saved.getInputType());
    }

    @Test
    @DisplayName("duplicate key — throws DuplicateResourceException")
    void execute_duplicateKey_throws() {
        fakeLoad.store(traitWith("comfort_level"));

        assertThrows(DuplicateResourceException.class,
                () -> service.execute("comfort_level", "reviews.traits.comfortLevel", ReviewTraitInputType.SLIDER));
    }

    @Test
    @DisplayName("blank key — throws IllegalArgumentException (domain validation)")
    void execute_blankKey_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute("  ", "reviews.traits.something", ReviewTraitInputType.SLIDER));
    }

    @Test
    @DisplayName("key with uppercase — throws IllegalArgumentException (domain validation)")
    void execute_invalidKeyChars_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute("ComfortLevel", "reviews.traits.comfortLevel", ReviewTraitInputType.SLIDER));
    }

    @Test
    @DisplayName("key with spaces — throws IllegalArgumentException (domain validation)")
    void execute_keyWithSpaces_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute("comfort level", "reviews.traits.comfortLevel", ReviewTraitInputType.SLIDER));
    }

    @Test
    @DisplayName("blank labelI18n — throws IllegalArgumentException (domain validation)")
    void execute_blankLabel_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute("comfort_level", "  ", ReviewTraitInputType.SLIDER));
    }

    @Test
    @DisplayName("null inputType — throws NullPointerException (domain validation)")
    void execute_nullInputType_throws() {
        assertThrows(Exception.class,
                () -> service.execute("comfort_level", "reviews.traits.comfortLevel", null));
    }

    @Test
    @DisplayName("RADIO input type — persisted correctly")
    void execute_radioInputType() {
        service.execute("fit_type", "reviews.traits.fitType", ReviewTraitInputType.RADIO);

        assertEquals(ReviewTraitInputType.RADIO, fakeSave.savedTraits.get(0).getInputType());
    }

    // =========================================================
    //  Fakes
    // =========================================================

    private static ReviewTrait traitWith(String key) {
        return new ReviewTrait(null, key, "label", ReviewTraitInputType.SLIDER, null, null);
    }

    static class FakeLoadReviewTraitPort implements LoadReviewTraitPort {
        private final Map<Long, ReviewTrait>   byId  = new HashMap<>();
        private final Map<String, ReviewTrait> byKey = new HashMap<>();

        void store(ReviewTrait trait) {
            if (trait.getId() != null) byId.put(trait.getId(), trait);
            byKey.put(trait.getKey(), trait);
        }

        @Override public Optional<ReviewTrait> findById(Long id)          { return Optional.ofNullable(byId.get(id)); }
        @Override public Optional<ReviewTrait> findByKey(String key)       { return Optional.ofNullable(byKey.get(key)); }
        @Override public List<ReviewTrait>     findAll()                   { return List.copyOf(byId.values()); }
        @Override public List<ReviewTrait>     findAllByIds(Collection<Long> ids) {
            return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        }
        @Override public boolean existsByKey(String key) { return byKey.containsKey(key); }
    }

    static class FakeSaveReviewTraitPort implements SaveReviewTraitPort {
        private final AtomicLong   idSeq      = new AtomicLong(1);
        final List<ReviewTrait>    savedTraits = new ArrayList<>();

        @Override
        public ReviewTrait save(ReviewTrait trait) {
            ReviewTrait persisted = new ReviewTrait(
                    trait.getId() != null ? trait.getId() : idSeq.getAndIncrement(),
                    trait.getKey(), trait.getLabelI18n(), trait.getInputType(),
                    trait.getCreatedAt(), trait.getUpdatedAt());
            savedTraits.add(persisted);
            return persisted;
        }

        @Override
        public void deleteById(Long id) { savedTraits.removeIf(t -> id.equals(t.getId())); }
    }
}
