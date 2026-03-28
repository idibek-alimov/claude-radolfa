package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.review.GetPendingReviewsUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.readmodel.ReviewAdminView;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Review;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GetPendingReviewsService implements GetPendingReviewsUseCase {

    private final LoadReviewPort          loadReviewPort;
    private final LoadListingVariantPort  loadListingVariantPort;

    public GetPendingReviewsService(LoadReviewPort loadReviewPort,
                                    LoadListingVariantPort loadListingVariantPort) {
        this.loadReviewPort         = loadReviewPort;
        this.loadListingVariantPort = loadListingVariantPort;
    }

    @Override
    public List<ReviewAdminView> getPending(int limit) {
        List<Review> pending = loadReviewPort.findPendingOldestFirst(limit);

        Set<Long> variantIds = pending.stream()
                .map(Review::getListingVariantId)
                .collect(Collectors.toSet());

        Map<Long, ListingVariant> variantMap = loadListingVariantPort.findVariantsByIds(variantIds);

        return pending.stream()
                .map(review -> toAdminView(review, variantMap))
                .toList();
    }

    private ReviewAdminView toAdminView(Review review, Map<Long, ListingVariant> variantMap) {
        ListingVariant variant = variantMap.get(review.getListingVariantId());
        String slug = variant != null ? variant.getSlug() : "unknown";

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
