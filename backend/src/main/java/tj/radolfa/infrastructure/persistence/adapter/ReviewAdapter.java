package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.SaveReviewPort;
import tj.radolfa.domain.model.Review;
import tj.radolfa.domain.model.ReviewStatus;
import tj.radolfa.infrastructure.persistence.entity.ReviewEntity;
import tj.radolfa.infrastructure.persistence.entity.ReviewPhotoEntity;
import tj.radolfa.infrastructure.persistence.mappers.ReviewMapper;
import tj.radolfa.infrastructure.persistence.repository.ReviewRepository;

import java.util.List;
import java.util.Optional;

@Component
public class ReviewAdapter implements LoadReviewPort, SaveReviewPort {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper mapper;

    public ReviewAdapter(ReviewRepository reviewRepository, ReviewMapper mapper) {
        this.reviewRepository = reviewRepository;
        this.mapper           = mapper;
    }

    // ---- LoadReviewPort ------------------------------------------------

    @Override
    public Optional<Review> findById(Long id) {
        return reviewRepository.findById(id).map(mapper::toReview);
    }

    @Override
    public boolean existsByOrderAndVariant(Long orderId, Long listingVariantId) {
        return reviewRepository.existsByOrderIdAndListingVariantId(orderId, listingVariantId);
    }

    @Override
    public List<Review> findAllApprovedByVariant(Long listingVariantId) {
        return reviewRepository
                .findByListingVariantIdAndStatus(listingVariantId, ReviewStatus.APPROVED.name())
                .stream()
                .map(mapper::toReview)
                .toList();
    }

    @Override
    public Page<Review> findApprovedByVariant(Long listingVariantId, Pageable pageable) {
        return reviewRepository
                .findByListingVariantIdAndStatus(listingVariantId, ReviewStatus.APPROVED.name(), pageable)
                .map(mapper::toReview);
    }

    @Override
    public List<Review> findPendingOldestFirst(int limit) {
        return reviewRepository
                .findByStatusOrderByCreatedAtAsc(ReviewStatus.PENDING.name(), PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(mapper::toReview)
                .toList();
    }

    // ---- SaveReviewPort ------------------------------------------------

    @Override
    public Review save(Review review) {
        ReviewEntity entity;

        if (review.getId() != null) {
            entity = reviewRepository.findById(review.getId())
                    .orElseThrow(() -> new IllegalStateException("Review not found: " + review.getId()));
            // Update mutable fields
            entity.setStatus(review.getStatus());
            entity.setSellerReply(review.getSellerReply());
            entity.setSellerRepliedAt(review.getSellerRepliedAt());
        } else {
            entity = mapper.toEntity(review);
        }

        // Rebuild photos list (clear + rebuild preserves orphanRemoval)
        entity.getPhotos().clear();
        List<String> photoUrls = review.getPhotos();
        for (int i = 0; i < photoUrls.size(); i++) {
            entity.getPhotos().add(new ReviewPhotoEntity(entity, photoUrls.get(i), i));
        }

        return mapper.toReview(reviewRepository.save(entity));
    }
}
