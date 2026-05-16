package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.loyalty.AwardReviewBonusUseCase;
import tj.radolfa.application.ports.in.review.RecalculateRatingSummaryUseCase;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.ReviewFilter;
import tj.radolfa.application.ports.out.SaveReviewPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.MatchingSize;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.Review;
import tj.radolfa.domain.model.ReviewStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ModerateReviewServiceTest {

    private FakeLoadReviewPort     fakeLoad;
    private FakeSaveReviewPort     fakeSave;
    private FakeRecalcUseCase      fakeRecalc;
    private FakeAwardBonus         fakeAward;
    private FakeNotificationPort   fakeNotify;
    private ModerateReviewService  service;

    @BeforeEach
    void setUp() {
        fakeLoad   = new FakeLoadReviewPort();
        fakeSave   = new FakeSaveReviewPort();
        fakeRecalc = new FakeRecalcUseCase();
        fakeAward  = new FakeAwardBonus();
        fakeNotify = new FakeNotificationPort();
        service    = new ModerateReviewService(fakeLoad, fakeSave, fakeRecalc, fakeAward, fakeNotify);
    }

    // =========================================================
    //  approve — happy path
    // =========================================================

    @Test
    @DisplayName("First approval: sets status APPROVED, marks pointsAwardedAt, recalcs, awards bonus, notifies")
    void approve_firstTime_allSideEffectsFire() {
        fakeLoad.review = pendingReview(1L);

        service.approve(1L);

        assertEquals(ReviewStatus.APPROVED, fakeSave.saved.getStatus());
        assertNotNull(fakeSave.saved.getPointsAwardedAt());
        assertTrue(fakeRecalc.executed);
        assertEquals(1, fakeAward.calls);
        assertEquals(1, fakeNotify.approvedNotifications.size());
        assertEquals(0, fakeNotify.replyNotifications.size());
    }

    @Test
    @DisplayName("approve on already-APPROVED review is a no-op: no save, no recalc, no award, no notify")
    void approve_alreadyApproved_isNoOp() {
        fakeLoad.review = approvedReview(1L);

        service.approve(1L);

        assertNull(fakeSave.saved);
        assertFalse(fakeRecalc.executed);
        assertEquals(0, fakeAward.calls);
        assertEquals(0, fakeNotify.approvedNotifications.size());
    }

    @Test
    @DisplayName("reject does not award bonus or send notifications")
    void reject_doesNotAwardOrNotify() {
        fakeLoad.review = pendingReview(1L);

        service.reject(1L);

        assertEquals(ReviewStatus.REJECTED, fakeSave.saved.getStatus());
        assertEquals(0, fakeAward.calls);
        assertEquals(0, fakeNotify.approvedNotifications.size());
    }

    @Test
    @DisplayName("approve throws ResourceNotFoundException when review not found")
    void approve_reviewNotFound_throws() {
        fakeLoad.review = null;

        assertThrows(ResourceNotFoundException.class, () -> service.approve(99L));
        assertNull(fakeSave.saved);
        assertEquals(0, fakeAward.calls);
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private static Review pendingReview(Long id) {
        return new Review(id, 1L, null, 10L, 42L,
                "Alice", 5, "Great", "Really loved it",
                null, null, MatchingSize.ACCURATE, List.of(),
                ReviewStatus.PENDING, null, null,
                Instant.now(), Instant.now(), null, null);
    }

    private static Review approvedReview(Long id) {
        return new Review(id, 1L, null, 10L, 42L,
                "Alice", 5, "Great", "Really loved it",
                null, null, MatchingSize.ACCURATE, List.of(),
                ReviewStatus.APPROVED, null, null,
                Instant.now(), Instant.now(), null, Instant.now());
    }

    // =========================================================
    //  In-memory fakes
    // =========================================================

    static class FakeLoadReviewPort implements LoadReviewPort {
        Review review;

        @Override
        public Optional<Review> findById(Long id) { return Optional.ofNullable(review); }

        @Override public boolean existsByOrderAndVariant(Long o, Long v) { return false; }
        @Override public List<Review> findAllApprovedByVariant(Long id) { return List.of(); }
        @Override public Page<Review> findApprovedByVariant(Long id, ReviewFilter f, Pageable p) { return Page.empty(); }
        @Override public List<Review> findPendingOldestFirst(int limit) { return List.of(); }
        @Override public org.springframework.data.domain.Page<Review> findAllForAdmin(tj.radolfa.domain.model.ReviewStatus status, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
    }

    static class FakeSaveReviewPort implements SaveReviewPort {
        Review saved;

        @Override
        public Review save(Review review) {
            saved = review;
            return review;
        }
    }

    static class FakeRecalcUseCase implements RecalculateRatingSummaryUseCase {
        boolean executed;

        @Override
        public void execute(Long variantId) { executed = true; }
    }

    static class FakeAwardBonus implements AwardReviewBonusUseCase {
        int calls;

        @Override
        public void execute(Long userId) { calls++; }
    }

    static class FakeNotificationPort implements NotificationPort {
        List<Long> approvedNotifications = new ArrayList<>();
        List<Long> replyNotifications    = new ArrayList<>();

        @Override public void sendOrderConfirmation(Long userId, Long orderId) {}
        @Override public void sendOrderStatusUpdate(Long userId, Long orderId, OrderStatus status) {}

        @Override
        public void sendReviewApprovedNotification(Long userId, Long reviewId) {
            approvedNotifications.add(reviewId);
        }

        @Override
        public void sendReviewReplyNotification(Long userId, Long reviewId) {
            replyNotifications.add(reviewId);
        }

        @Override public void sendDeliveryCode(Long u, Long o, String c, java.time.Instant e) {}
    }
}
