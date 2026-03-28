package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.readmodel.ReviewAdminView;
import tj.radolfa.domain.model.Review;

import java.util.List;

@Service
public class GetPendingReviewsService {

    private final LoadReviewPort          loadReviewPort;
    private final LoadListingVariantPort  loadListingVariantPort;

    public GetPendingReviewsService(LoadReviewPort loadReviewPort,
                                    LoadListingVariantPort loadListingVariantPort) {
        this.loadReviewPort         = loadReviewPort;
        this.loadListingVariantPort = loadListingVariantPort;
    }

    public List<ReviewAdminView> getPending(int limit) {
        return loadReviewPort.findPendingOldestFirst(limit).stream()
                .map(this::toAdminView)
                .toList();
    }

    private ReviewAdminView toAdminView(Review review) {
        String slug = loadListingVariantPort.findVariantById(review.getListingVariantId())
                .map(v -> v.getSlug())
                .orElse("unknown");

        return new ReviewAdminView(
                review.getId(),
                review.getListingVariantId(),
                slug,
                review.getAuthorName(),
                review.getRating(),
                review.getTitle(),
                review.getBody(),
                review.getPros(),
                review.getCons(),
                review.getMatchingSize(),
                review.getPhotos(),
                review.getStatus(),
                review.getSellerReply(),
                review.getCreatedAt()
        );
    }
}
