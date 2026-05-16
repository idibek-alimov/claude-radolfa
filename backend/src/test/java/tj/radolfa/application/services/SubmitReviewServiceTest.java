package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.review.SubmitReviewUseCase;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.LoadReviewTraitPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveReviewPort;
import tj.radolfa.application.ports.out.VerifyPurchasePort;
import tj.radolfa.domain.exception.UnauthorizedReviewException;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.MatchingSize;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.Review;
import tj.radolfa.domain.model.ReviewStatus;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.domain.model.ReviewTraitInputType;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class SubmitReviewServiceTest {

    // ---- Fakes ----

    private FakeLoadUserPort        fakeUser;
    private FakeVerifyPurchasePort  fakePurchase;
    private FakeLoadReviewPort      fakeReview;
    private FakeSaveReviewPort      fakeSave;
    private FakeLoadReviewTraitPort fakeTrait;
    private SubmitReviewService     service;

    @BeforeEach
    void setUp() {
        fakeUser     = new FakeLoadUserPort();
        fakePurchase = new FakeVerifyPurchasePort(true);
        fakeReview   = new FakeLoadReviewPort(false);
        fakeSave     = new FakeSaveReviewPort();
        fakeTrait    = new FakeLoadReviewTraitPort(List.of(
                new ReviewTrait(1L, "comfort_level", "Comfort Level",
                        ReviewTraitInputType.SLIDER, Instant.now(), Instant.now())
        ));

        service = new SubmitReviewService(fakeUser, fakePurchase, fakeReview, fakeSave, fakeTrait);
    }

    // =========================================================
    //  Photo wiring tests
    // =========================================================

    @Test
    @DisplayName("Photos are persisted in submission order when three URLs are provided")
    void submit_withPhotos_persistsAllUrlsInOrder() {
        List<String> photos = List.of("a.jpg", "b.jpg", "c.jpg");

        service.execute(command(photos));

        assertEquals(photos, fakeSave.lastSaved.getPhotos());
    }

    @Test
    @DisplayName("A single photo URL is persisted")
    void submit_withSinglePhoto_persistsIt() {
        service.execute(command(List.of("x.jpg")));

        assertEquals(1, fakeSave.lastSaved.getPhotos().size());
        assertEquals("x.jpg", fakeSave.lastSaved.getPhotos().get(0));
    }

    @Test
    @DisplayName("Null photoUrls falls back to an empty list without throwing")
    void submit_withNullPhotoUrls_fallsBackToEmptyList() {
        assertDoesNotThrow(() -> service.execute(command(null)));
        assertTrue(fakeSave.lastSaved.getPhotos().isEmpty());
    }

    @Test
    @DisplayName("Empty photoUrls list results in no photos persisted")
    void submit_withEmptyPhotoUrls_persistsEmptyList() {
        service.execute(command(List.of()));

        assertTrue(fakeSave.lastSaved.getPhotos().isEmpty());
    }

    @Test
    @DisplayName("Unverified purchase throws UnauthorizedReviewException regardless of photos")
    void submit_unverifiedPurchase_throwsUnauthorizedReviewException() {
        fakePurchase = new FakeVerifyPurchasePort(false);
        service = new SubmitReviewService(fakeUser, fakePurchase, fakeReview, fakeSave, fakeTrait);

        assertThrows(UnauthorizedReviewException.class,
                () -> service.execute(command(List.of("should-not-persist.jpg"))));
        assertNull(fakeSave.lastSaved);
    }

    // =========================================================
    //  Trait validation tests
    // =========================================================

    @Test
    @DisplayName("Valid SLIDER trait answer is persisted")
    void submit_withValidSliderTrait_persistsTrait() {
        service.execute(commandWithTraits(Map.of("comfort_level", 4)));

        assertEquals(Map.of("comfort_level", 4), fakeSave.lastSaved.getTraitAnswers());
    }

    @Test
    @DisplayName("Null traitAnswers results in empty map without throwing")
    void submit_withNullTraitAnswers_persistsEmptyMap() {
        assertDoesNotThrow(() -> service.execute(commandWithTraits(null)));
        assertTrue(fakeSave.lastSaved.getTraitAnswers().isEmpty());
    }

    @Test
    @DisplayName("Unknown trait key throws IllegalArgumentException")
    void submit_withUnknownTraitKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(commandWithTraits(Map.of("unknown_key", 3))));
        assertNull(fakeSave.lastSaved);
    }

    @Test
    @DisplayName("SLIDER value above 5 throws IllegalArgumentException")
    void submit_withSliderValueAboveFive_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(commandWithTraits(Map.of("comfort_level", 6))));
        assertNull(fakeSave.lastSaved);
    }

    @Test
    @DisplayName("SLIDER value below 1 throws IllegalArgumentException")
    void submit_withSliderValueBelowOne_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(commandWithTraits(Map.of("comfort_level", 0))));
        assertNull(fakeSave.lastSaved);
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private static SubmitReviewUseCase.Command command(List<String> photoUrls) {
        return new SubmitReviewUseCase.Command(
                1L,               // listingVariantId
                10L,              // skuId
                100L,             // orderId
                42L,              // authorId
                5,                // rating
                "Great product",  // title
                "Really loved it!", // body
                null,             // pros
                null,             // cons
                MatchingSize.ACCURATE,
                photoUrls,
                null              // traitAnswers
        );
    }

    private static SubmitReviewUseCase.Command commandWithTraits(Map<String, Object> traitAnswers) {
        return new SubmitReviewUseCase.Command(
                1L, 10L, 100L, 42L, 5,
                "Great product", "Really loved it!",
                null, null, MatchingSize.ACCURATE,
                List.of(),
                traitAnswers
        );
    }

    // =========================================================
    //  In-memory fakes
    // =========================================================

    static class FakeLoadUserPort implements LoadUserPort {
        @Override
        public Optional<User> loadById(Long id) {
            return Optional.of(new User(id, new PhoneNumber("992000000000"),
                    UserRole.USER, "Alice", null, LoyaltyProfile.empty(), true, 1L));
        }
        @Override public Optional<User> loadByPhone(String phone) { return Optional.empty(); }
        @Override public List<User> findAllNonPermanent() { return List.of(); }
            @Override public List<User> findByRoleAndEnabledTrue(tj.radolfa.domain.model.UserRole r) { return List.of(); }
    }

    static class FakeVerifyPurchasePort implements VerifyPurchasePort {
        private final boolean result;
        FakeVerifyPurchasePort(boolean result) { this.result = result; }
        @Override
        public boolean hasPurchasedVariant(Long userId, Long listingVariantId) { return result; }
    }

    static class FakeLoadReviewPort implements LoadReviewPort {
        private final boolean duplicateExists;
        FakeLoadReviewPort(boolean duplicateExists) { this.duplicateExists = duplicateExists; }

        @Override
        public boolean existsByOrderAndVariant(Long orderId, Long listingVariantId) { return duplicateExists; }
        @Override public Optional<Review> findById(Long id) { return Optional.empty(); }
        @Override public List<Review> findAllApprovedByVariant(Long listingVariantId) { return List.of(); }
        @Override public Page<Review> findApprovedByVariant(Long listingVariantId, tj.radolfa.application.ports.out.ReviewFilter filter, Pageable pageable) { return Page.empty(); }
        @Override public List<Review> findPendingOldestFirst(int limit) { return List.of(); }
        @Override public org.springframework.data.domain.Page<Review> findAllForAdmin(tj.radolfa.domain.model.ReviewStatus status, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
    }

    static class FakeLoadReviewTraitPort implements LoadReviewTraitPort {
        private final List<ReviewTrait> traits;
        FakeLoadReviewTraitPort(List<ReviewTrait> traits) { this.traits = traits; }

        @Override public Optional<ReviewTrait> findById(Long id) { return Optional.empty(); }
        @Override public Optional<ReviewTrait> findByKey(String key) { return Optional.empty(); }
        @Override public List<ReviewTrait> findAll() { return traits; }
        @Override public List<ReviewTrait> findAllByIds(Collection<Long> ids) { return List.of(); }
        @Override public boolean existsByKey(String key) { return false; }
        @Override public List<ReviewTrait> findByVariantId(Long listingVariantId) { return traits; }
    }

    static class FakeSaveReviewPort implements SaveReviewPort {
        Review lastSaved;
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public Review save(Review review) {
            lastSaved = new Review(
                    idGen.getAndIncrement(),
                    review.getListingVariantId(),
                    review.getSkuId(),
                    review.getOrderId(),
                    review.getAuthorId(),
                    review.getAuthorName(),
                    review.getRating(),
                    review.getTitle(),
                    review.getBody(),
                    review.getPros(),
                    review.getCons(),
                    review.getMatchingSize(),
                    review.getPhotos(),
                    review.getStatus() != null ? review.getStatus() : ReviewStatus.PENDING,
                    review.getSellerReply(),
                    review.getSellerRepliedAt(),
                    review.getCreatedAt(),
                    review.getUpdatedAt(),
                    review.getTraitAnswers().isEmpty() ? null : review.getTraitAnswers(),
                    review.getPointsAwardedAt()
            );
            return lastSaved;
        }
    }
}
