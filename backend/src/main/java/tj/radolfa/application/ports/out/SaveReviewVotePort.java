package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.VoteType;

import java.util.Optional;

public interface SaveReviewVotePort {

    /**
     * Upserts a vote — replaces any previous vote the user cast on this review.
     *
     * @return the user's previous vote on this review, or empty if this is their first vote.
     */
    Optional<VoteType> saveVote(Long reviewId, Long userId, VoteType vote);
}
