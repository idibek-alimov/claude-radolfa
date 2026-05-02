package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.out.LoadRatingSummaryPort;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.LoadReviewTraitPort;
import tj.radolfa.application.ports.out.ReviewFilter;
import tj.radolfa.application.ports.out.SaveRatingSummaryPort;
import tj.radolfa.domain.model.Review;
import tj.radolfa.domain.model.ReviewStatus;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.domain.model.ReviewTraitInputType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RecalculateRatingSummaryServiceTest {

    private FakeLoadReviewPort      fakeReview;
    private FakeLoadReviewTraitPort fakeTrait;
    private FakeSaveRatingSummaryPort fakeSave;
    private RecalculateRatingSummaryService service;

    @BeforeEach
    void setUp() {
        fakeReview = new FakeLoadReviewPort();
        fakeTrait  = new FakeLoadReviewTraitPort();
        fakeSave   = new FakeSaveRatingSummaryPort();
        service    = new RecalculateRatingSummaryService(fakeReview, fakeTrait, fakeSave);
    }

    @Test
    @DisplayName("Three SLIDER answers average to 4.00 with count 3")
    void execute_threeSliderAnswers_aggregatesCorrectly() {
        fakeTrait.traits = List.of(
                new ReviewTrait(1L, "comfort_level", "Comfort Level",
                        ReviewTraitInputType.SLIDER, Instant.now(), Instant.now())
        );
        fakeReview.reviews = List.of(
                review(5, Map.of("comfort_level", 4)),
                review(4, Map.of("comfort_level", 5)),
                review(3, Map.of("comfort_level", 3))
        );

        service.execute(1L);

        List<LoadRatingSummaryPort.TraitAggregateView> aggs = fakeSave.lastTraitAggregates;
        assertEquals(1, aggs.size());

        LoadRatingSummaryPort.TraitAggregateView agg = aggs.get(0);
        assertEquals("comfort_level", agg.traitKey());
        assertEquals(new BigDecimal("4.00"), agg.average());
        assertEquals(3, agg.count());
        assertEquals(ReviewTraitInputType.SLIDER, agg.inputType());
    }

    @Test
    @DisplayName("RADIO trait answer is excluded from aggregation")
    void execute_radioTraitAnswer_isExcludedFromAggregation() {
        fakeTrait.traits = List.of(
                new ReviewTrait(1L, "comfort_level", "Comfort Level",
                        ReviewTraitInputType.SLIDER, Instant.now(), Instant.now()),
                new ReviewTrait(2L, "size_fit", "Size Fit",
                        ReviewTraitInputType.RADIO, Instant.now(), Instant.now())
        );
        fakeReview.reviews = List.of(
                review(5, Map.of("comfort_level", 5, "size_fit", "ACCURATE")),
                review(4, Map.of("comfort_level", 3, "size_fit", "RUNS_SMALL"))
        );

        service.execute(1L);

        List<LoadRatingSummaryPort.TraitAggregateView> aggs = fakeSave.lastTraitAggregates;
        assertEquals(1, aggs.size(), "Only SLIDER traits should produce aggregates");
        assertEquals("comfort_level", aggs.get(0).traitKey());
        assertEquals(new BigDecimal("4.00"), aggs.get(0).average());
    }

    @Test
    @DisplayName("No approved reviews produces empty trait aggregates")
    void execute_noApprovedReviews_emptyAggregates() {
        fakeTrait.traits = List.of(
                new ReviewTrait(1L, "comfort_level", "Comfort Level",
                        ReviewTraitInputType.SLIDER, Instant.now(), Instant.now())
        );
        fakeReview.reviews = List.of();

        service.execute(1L);

        assertTrue(fakeSave.lastTraitAggregates.isEmpty());
    }

    @Test
    @DisplayName("Reviews missing a trait answer are skipped for that trait")
    void execute_reviewsWithMissingAnswer_skippedForThatTrait() {
        fakeTrait.traits = List.of(
                new ReviewTrait(1L, "comfort_level", "Comfort Level",
                        ReviewTraitInputType.SLIDER, Instant.now(), Instant.now())
        );
        fakeReview.reviews = List.of(
                review(5, Map.of("comfort_level", 4)),
                review(4, Map.of())   // no trait answer — should be skipped
        );

        service.execute(1L);

        List<LoadRatingSummaryPort.TraitAggregateView> aggs = fakeSave.lastTraitAggregates;
        assertEquals(1, aggs.size());
        assertEquals(new BigDecimal("4.00"), aggs.get(0).average());
        assertEquals(1, aggs.get(0).count());
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private static Review review(int rating, Map<String, Object> traitAnswers) {
        return new Review(null, 1L, null, 1L, 1L, "Tester",
                rating, null, "Good product", null, null,
                null, List.of(), ReviewStatus.APPROVED,
                null, null, null, null,
                traitAnswers, null);
    }

    // =========================================================
    //  In-memory fakes
    // =========================================================

    static class FakeLoadReviewPort implements LoadReviewPort {
        List<Review> reviews = new ArrayList<>();

        @Override
        public List<Review> findAllApprovedByVariant(Long listingVariantId) { return reviews; }
        @Override public Optional<Review> findById(Long id) { return Optional.empty(); }
        @Override public boolean existsByOrderAndVariant(Long orderId, Long listingVariantId) { return false; }
        @Override public Page<Review> findApprovedByVariant(Long listingVariantId, ReviewFilter filter, Pageable pageable) { return Page.empty(); }
        @Override public List<Review> findPendingOldestFirst(int limit) { return List.of(); }
        @Override public org.springframework.data.domain.Page<Review> findAllForAdmin(tj.radolfa.domain.model.ReviewStatus status, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
    }

    static class FakeLoadReviewTraitPort implements LoadReviewTraitPort {
        List<ReviewTrait> traits = new ArrayList<>();

        @Override public Optional<ReviewTrait> findById(Long id) { return Optional.empty(); }
        @Override public Optional<ReviewTrait> findByKey(String key) { return Optional.empty(); }
        @Override public List<ReviewTrait> findAll() { return traits; }
        @Override public List<ReviewTrait> findAllByIds(Collection<Long> ids) { return List.of(); }
        @Override public boolean existsByKey(String key) { return false; }
        @Override public List<ReviewTrait> findByVariantId(Long listingVariantId) { return traits; }
    }

    static class FakeSaveRatingSummaryPort implements SaveRatingSummaryPort {
        List<LoadRatingSummaryPort.TraitAggregateView> lastTraitAggregates = List.of();

        @Override
        public void upsert(Long listingVariantId,
                           BigDecimal averageRating,
                           int reviewCount,
                           Map<Integer, Integer> distribution,
                           int sizeAccurate,
                           int sizeRunsSmall,
                           int sizeRunsLarge,
                           List<LoadRatingSummaryPort.TraitAggregateView> traitAggregates) {
            this.lastTraitAggregates = traitAggregates;
        }
    }
}
