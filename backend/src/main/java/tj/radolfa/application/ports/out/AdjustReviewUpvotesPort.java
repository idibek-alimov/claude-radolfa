package tj.radolfa.application.ports.out;

public interface AdjustReviewUpvotesPort {

    /** Atomically adds {@code delta} to the review's upvotes counter. */
    void adjust(Long reviewId, int delta);
}
