package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

class ReplyToReviewServiceTest {

    private FakeLoadReviewPort   fakeLoad;
    private FakeSaveReviewPort   fakeSave;
    private FakeNotificationPort fakeNotify;
    private ReplyToReviewService service;

    @BeforeEach
    void setUp() {
        fakeLoad   = new FakeLoadReviewPort();
        fakeSave   = new FakeSaveReviewPort();
        fakeNotify = new FakeNotificationPort();
        service    = new ReplyToReviewService(fakeLoad, fakeSave, fakeNotify);
    }

    @Test
    @DisplayName("First reply: saves the reply text and sends a notification")
    void firstReply_savesAndNotifies() {
        fakeLoad.review = approvedReviewNoReply(1L);

        service.execute(1L, "Thanks for your feedback!");

        assertEquals("Thanks for your feedback!", fakeSave.saved.getSellerReply());
        assertEquals(1, fakeNotify.replyNotifications.size());
        assertEquals(1L, fakeNotify.replyNotifications.get(0));
    }

    @Test
    @DisplayName("Second reply (edit): updates reply text but does NOT send another notification")
    void secondReply_savesButDoesNotNotify() {
        fakeLoad.review = approvedReviewWithReply(1L, "Original reply");

        service.execute(1L, "Updated reply");

        assertEquals("Updated reply", fakeSave.saved.getSellerReply());
        assertEquals(0, fakeNotify.replyNotifications.size());
    }

    @Test
    @DisplayName("Reply to a PENDING review throws IllegalStateException")
    void replyToPendingReview_throwsAndDoesNotNotify() {
        fakeLoad.review = pendingReview(1L);

        assertThrows(IllegalStateException.class,
                () -> service.execute(1L, "This should not work"));

        assertNull(fakeSave.saved);
        assertEquals(0, fakeNotify.replyNotifications.size());
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when review does not exist")
    void execute_reviewNotFound_throws() {
        fakeLoad.review = null;

        assertThrows(ResourceNotFoundException.class,
                () -> service.execute(99L, "reply"));
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private static Review approvedReviewNoReply(Long id) {
        return new Review(id, 1L, null, 10L, 42L,
                "Alice", 5, "Great", "Loved it",
                null, null, MatchingSize.ACCURATE, List.of(),
                ReviewStatus.APPROVED, null, null,
                Instant.now(), Instant.now(), null, null);
    }

    private static Review approvedReviewWithReply(Long id, String existingReply) {
        return new Review(id, 1L, null, 10L, 42L,
                "Alice", 5, "Great", "Loved it",
                null, null, MatchingSize.ACCURATE, List.of(),
                ReviewStatus.APPROVED, existingReply, Instant.now(),
                Instant.now(), Instant.now(), null, null);
    }

    private static Review pendingReview(Long id) {
        return new Review(id, 1L, null, 10L, 42L,
                "Alice", 5, "Great", "Loved it",
                null, null, MatchingSize.ACCURATE, List.of(),
                ReviewStatus.PENDING, null, null,
                Instant.now(), Instant.now(), null, null);
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

    static class FakeNotificationPort implements NotificationPort {
        List<Long> replyNotifications = new ArrayList<>();

        @Override public void sendOrderConfirmation(Long userId, Long orderId) {}
        @Override public void sendOrderStatusUpdate(Long userId, Long orderId, OrderStatus status) {}
        @Override public void sendReviewApprovedNotification(Long userId, Long reviewId) {}

        @Override
        public void sendReviewReplyNotification(Long userId, Long reviewId) {
            replyNotifications.add(reviewId);
        }

        @Override public void sendDeliveryCode(Long u, Long o, String c, java.time.Instant e) {}
        @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
        @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
    }
}
