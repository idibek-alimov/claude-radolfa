package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.Review;

import java.util.List;
import java.util.Optional;

public interface LoadReviewPort {

    Optional<Review> findById(Long id);

    boolean existsByOrderAndVariant(Long orderId, Long listingVariantId);

    /** Returns ALL approved reviews for a variant — used by rating recalculation. Never paginated. */
    List<Review> findAllApprovedByVariant(Long listingVariantId);

    /** Paginated approved reviews for storefront display. */
    Page<Review> findApprovedByVariant(Long listingVariantId, boolean hasPhotos, Pageable pageable);

    /** Convenience overload — no photo filter applied. */
    default Page<Review> findApprovedByVariant(Long listingVariantId, Pageable pageable) {
        return findApprovedByVariant(listingVariantId, false, pageable);
    }

    /** Returns the oldest pending reviews up to {@code limit} — used by the admin moderation queue. */
    List<Review> findPendingOldestFirst(int limit);
}
