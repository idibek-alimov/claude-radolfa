package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.VoteType;

public interface SaveReviewVotePort {

    /** Upserts a vote — replaces any previous vote the user cast on this review. */
    void saveVote(Long reviewId, Long userId, VoteType vote);
}
