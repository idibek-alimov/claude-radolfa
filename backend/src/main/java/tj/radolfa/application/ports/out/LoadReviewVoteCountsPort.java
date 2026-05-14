package tj.radolfa.application.ports.out;

import java.util.List;
import java.util.Map;

public interface LoadReviewVoteCountsPort {

    /**
     * Returns vote counts for the given review IDs in a single query.
     *
     * <p>The returned map key is the review ID; the value is a two-element int array:
     * {@code [helpfulCount, notHelpfulCount]}.  IDs with no votes are absent from the map.
     */
    Map<Long, int[]> findVoteCountsByReviewIds(List<Long> reviewIds);
}
