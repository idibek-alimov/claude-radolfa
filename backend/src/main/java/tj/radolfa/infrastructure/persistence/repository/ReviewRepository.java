package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.domain.model.ReviewStatus;
import tj.radolfa.infrastructure.persistence.entity.ReviewEntity;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    List<ReviewEntity> findByListingVariantIdAndStatus(Long listingVariantId, ReviewStatus status);

    boolean existsByOrderIdAndListingVariantId(Long orderId, Long listingVariantId);

    Page<ReviewEntity> findByStatusOrderByCreatedAtAsc(ReviewStatus status, Pageable pageable);

    /** Paginated approved reviews with optional hasPhotos, rating, and search filters. */
    @Query("""
            SELECT r FROM ReviewEntity r
            WHERE r.listingVariantId = :variantId
              AND r.status = :status
              AND (:hasPhotos = false OR SIZE(r.photos) > 0)
              AND (:rating IS NULL OR r.rating = :rating)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(r.body) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ReviewEntity> findApprovedFiltered(
            @Param("variantId") Long variantId,
            @Param("status") ReviewStatus status,
            @Param("hasPhotos") boolean hasPhotos,
            @Param("rating") Integer rating,
            @Param("search") String search,
            Pageable pageable);
}
