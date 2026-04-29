package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.review.SubmitReviewUseCase;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveReviewPort;
import tj.radolfa.application.ports.out.VerifyPurchasePort;
import tj.radolfa.domain.exception.UnauthorizedReviewException;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.MatchingSize;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.Review;
import tj.radolfa.domain.model.ReviewStatus;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class SubmitReviewServiceTest {

    // ---- Fakes ----

    private FakeLoadUserPort       fakeUser;
    private FakeVerifyPurchasePort fakePurchase;
    private FakeLoadReviewPort     fakeReview;
    private FakeSaveReviewPort     fakeSave;
    private SubmitReviewService    service;

    @BeforeEach
    void setUp() {
        fakeUser     = new FakeLoadUserPort();
        fakePurchase = new FakeVerifyPurchasePort(true);
        fakeReview   = new FakeLoadReviewPort(false);
        fakeSave     = new FakeSaveReviewPort();

        service = new SubmitReviewService(fakeUser, fakePurchase, fakeReview, fakeSave);
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
        service = new SubmitReviewService(fakeUser, fakePurchase, fakeReview, fakeSave);

        assertThrows(UnauthorizedReviewException.class,
                () -> service.execute(command(List.of("should-not-persist.jpg"))));
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
                photoUrls
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
                    review.getUpdatedAt()
            );
            return lastSaved;
        }
    }
}
