package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.ReviewEntity;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    List<ReviewEntity> findByListingVariantIdAndStatus(Long listingVariantId, String status);

    boolean existsByOrderIdAndListingVariantId(Long orderId, Long listingVariantId);

    Page<ReviewEntity> findByListingVariantIdAndStatus(Long listingVariantId, String status, Pageable pageable);

    Page<ReviewEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
