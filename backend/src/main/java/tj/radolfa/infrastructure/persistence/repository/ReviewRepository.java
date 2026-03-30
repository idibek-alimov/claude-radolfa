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

    Page<ReviewEntity> findByListingVariantIdAndStatus(Long listingVariantId, ReviewStatus status, Pageable pageable);

    Page<ReviewEntity> findByStatusOrderByCreatedAtAsc(ReviewStatus status, Pageable pageable);

    /** Approved reviews for a variant that have at least one photo attached. */
    @Query("SELECT r FROM ReviewEntity r " +
           "WHERE r.listingVariantId = :variantId AND r.status = :status AND SIZE(r.photos) > 0")
    Page<ReviewEntity> findByListingVariantIdAndStatusWithPhotos(
            @Param("variantId") Long variantId,
            @Param("status") ReviewStatus status,
            Pageable pageable);
}
