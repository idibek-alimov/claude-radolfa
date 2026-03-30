package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.out.LoadReviewVoteCountsPort;
import tj.radolfa.application.ports.out.SaveReviewVotePort;
import tj.radolfa.domain.model.VoteType;
import tj.radolfa.infrastructure.persistence.entity.ReviewVoteEntity;
import tj.radolfa.infrastructure.persistence.repository.ReviewVoteRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReviewVoteAdapter implements SaveReviewVotePort, LoadReviewVoteCountsPort {

    private final ReviewVoteRepository reviewVoteRepository;

    public ReviewVoteAdapter(ReviewVoteRepository reviewVoteRepository) {
        this.reviewVoteRepository = reviewVoteRepository;
    }

    // ---- SaveReviewVotePort --------------------------------------------

    @Override
    @Transactional
    public void saveVote(Long reviewId, Long userId, VoteType vote) {
        ReviewVoteEntity entity = reviewVoteRepository
                .findByReviewIdAndUserId(reviewId, userId)
                .orElse(new ReviewVoteEntity(null, reviewId, userId, vote, null));
        entity.setVote(vote);
        reviewVoteRepository.save(entity);
    }

    // ---- LoadReviewVoteCountsPort --------------------------------------

    @Override
    public Map<Long, int[]> findVoteCountsByReviewIds(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = reviewVoteRepository.findVoteCountsByReviewIds(reviewIds);
        Map<Long, int[]> result = new HashMap<>();
        for (Object[] row : rows) {
            long rid      = ((Number) row[0]).longValue();
            int helpful    = ((Number) row[1]).intValue();
            int notHelpful = ((Number) row[2]).intValue();
            result.put(rid, new int[]{helpful, notHelpful});
        }
        return result;
    }
}
