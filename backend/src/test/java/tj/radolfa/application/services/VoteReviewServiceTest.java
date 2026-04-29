package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.review.VoteReviewUseCase;
import tj.radolfa.application.ports.out.AdjustReviewUpvotesPort;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.ReviewFilter;
import tj.radolfa.application.ports.out.SaveReviewVotePort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.MatchingSize;
import tj.radolfa.domain.model.Review;
import tj.radolfa.domain.model.ReviewStatus;
import tj.radolfa.domain.model.VoteType;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VoteReviewServiceTest {

    private static final long REVIEW_ID = 1L;
    private static final long USER_ID   = 42L;

    private FakeLoadReviewPort          fakeLoad;
    private FakeSaveReviewVotePort      fakeVote;
    private FakeAdjustReviewUpvotesPort fakeAdjust;
    private VoteReviewService           service;

    @BeforeEach
    void setUp() {
        fakeLoad   = new FakeLoadReviewPort();
        fakeVote   = new FakeSaveReviewVotePort();
        fakeAdjust = new FakeAdjustReviewUpvotesPort();
        service    = new VoteReviewService(fakeLoad, fakeVote, fakeAdjust);
    }

    // ── Upvote counter delta tests ─────────────────────────────────────────

    @Test
    @DisplayName("First HELPFUL vote increments upvotes by 1")
    void firstHelpfulVoteIncrementsUpvotes() {
        fakeLoad.existingReview = stubReview();
        fakeVote.previousVote  = Optional.empty();

        service.execute(new VoteReviewUseCase.Command(REVIEW_ID, USER_ID, VoteType.HELPFUL));

        assertEquals(1, fakeAdjust.lastDelta);
    }

    @Test
    @DisplayName("First NOT_HELPFUL vote does not change upvotes")
    void firstNotHelpfulVoteNoChange() {
        fakeLoad.existingReview = stubReview();
        fakeVote.previousVote  = Optional.empty();

        service.execute(new VoteReviewUseCase.Command(REVIEW_ID, USER_ID, VoteType.NOT_HELPFUL));

        assertEquals(0, fakeAdjust.lastDelta);
    }

    @Test
    @DisplayName("HELPFUL → HELPFUL (repeat vote) is a no-op")
    void repeatHelpfulVoteIsNoOp() {
        fakeLoad.existingReview = stubReview();
        fakeVote.previousVote  = Optional.of(VoteType.HELPFUL);

        service.execute(new VoteReviewUseCase.Command(REVIEW_ID, USER_ID, VoteType.HELPFUL));

        assertEquals(0, fakeAdjust.lastDelta);
    }

    @Test
    @DisplayName("NOT_HELPFUL → HELPFUL increments upvotes by 1")
    void notHelpfulToHelpfulIncrements() {
        fakeLoad.existingReview = stubReview();
        fakeVote.previousVote  = Optional.of(VoteType.NOT_HELPFUL);

        service.execute(new VoteReviewUseCase.Command(REVIEW_ID, USER_ID, VoteType.HELPFUL));

        assertEquals(1, fakeAdjust.lastDelta);
    }

    @Test
    @DisplayName("HELPFUL → NOT_HELPFUL decrements upvotes by 1")
    void helpfulToNotHelpfulDecrements() {
        fakeLoad.existingReview = stubReview();
        fakeVote.previousVote  = Optional.of(VoteType.HELPFUL);

        service.execute(new VoteReviewUseCase.Command(REVIEW_ID, USER_ID, VoteType.NOT_HELPFUL));

        assertEquals(-1, fakeAdjust.lastDelta);
    }

    @Test
    @DisplayName("NOT_HELPFUL → NOT_HELPFUL is a no-op")
    void repeatNotHelpfulVoteIsNoOp() {
        fakeLoad.existingReview = stubReview();
        fakeVote.previousVote  = Optional.of(VoteType.NOT_HELPFUL);

        service.execute(new VoteReviewUseCase.Command(REVIEW_ID, USER_ID, VoteType.NOT_HELPFUL));

        assertEquals(0, fakeAdjust.lastDelta);
    }

    @Test
    @DisplayName("Voting on a non-existent review throws ResourceNotFoundException")
    void votingOnMissingReviewThrows() {
        fakeLoad.existingReview = null;

        assertThrows(ResourceNotFoundException.class,
                () -> service.execute(new VoteReviewUseCase.Command(REVIEW_ID, USER_ID, VoteType.HELPFUL)));
    }

    // ── Fakes ─────────────────────────────────────────────────────────────

    static class FakeLoadReviewPort implements LoadReviewPort {
        Review existingReview;

        @Override
        public Optional<Review> findById(Long id) {
            return Optional.ofNullable(existingReview);
        }
        @Override public boolean existsByOrderAndVariant(Long o, Long v) { return false; }
        @Override public List<Review> findAllApprovedByVariant(Long id) { return List.of(); }
        @Override public Page<Review> findApprovedByVariant(Long id, ReviewFilter f, Pageable p) { return Page.empty(); }
        @Override public List<Review> findPendingOldestFirst(int limit) { return List.of(); }
    }

    static class FakeSaveReviewVotePort implements SaveReviewVotePort {
        Optional<VoteType> previousVote = Optional.empty();
        Long lastReviewId;
        Long lastUserId;
        VoteType lastVote;

        @Override
        public Optional<VoteType> saveVote(Long reviewId, Long userId, VoteType vote) {
            lastReviewId = reviewId;
            lastUserId   = userId;
            lastVote     = vote;
            return previousVote;
        }
    }

    static class FakeAdjustReviewUpvotesPort implements AdjustReviewUpvotesPort {
        int lastDelta = Integer.MIN_VALUE;

        @Override
        public void adjust(Long reviewId, int delta) {
            lastDelta = delta;
        }
    }

    private static Review stubReview() {
        return new Review(
                REVIEW_ID, 1L, null, 10L, USER_ID,
                "Test User", 5, "Great", "Good product",
                null, null, MatchingSize.ACCURATE, List.of(),
                ReviewStatus.APPROVED, null, null,
                Instant.now(), Instant.now());
    }
}
