package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.ReviewVoteEntity;

import java.util.List;
import java.util.Optional;

public interface ReviewVoteRepository extends JpaRepository<ReviewVoteEntity, Long> {

    Optional<ReviewVoteEntity> findByReviewIdAndUserId(Long reviewId, Long userId);

    /**
     * Batch-loads helpful / not-helpful counts for a list of review IDs.
     * Each row: [review_id (Long), helpful_count (Long), not_helpful_count (Long)].
     */
    @Query(value = """
            SELECT review_id,
                   SUM(CASE WHEN vote = 'HELPFUL'     THEN 1 ELSE 0 END) AS helpful_count,
                   SUM(CASE WHEN vote = 'NOT_HELPFUL' THEN 1 ELSE 0 END) AS not_helpful_count
            FROM review_votes
            WHERE review_id IN :reviewIds
            GROUP BY review_id
            """,
            nativeQuery = true)
    List<Object[]> findVoteCountsByReviewIds(@Param("reviewIds") List<Long> reviewIds);
}
