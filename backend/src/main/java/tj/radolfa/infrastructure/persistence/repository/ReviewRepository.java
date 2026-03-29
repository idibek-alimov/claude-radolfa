package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.domain.model.ReviewStatus;
import tj.radolfa.infrastructure.persistence.entity.ReviewEntity;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    List<ReviewEntity> findByListingVariantIdAndStatus(Long listingVariantId, ReviewStatus status);

    boolean existsByOrderIdAndListingVariantId(Long orderId, Long listingVariantId);

    Page<ReviewEntity> findByListingVariantIdAndStatus(Long listingVariantId, ReviewStatus status, Pageable pageable);

    Page<ReviewEntity> findByStatusOrderByCreatedAtAsc(ReviewStatus status, Pageable pageable);
}
