package tj.radolfa.application.ports.in.review;

import tj.radolfa.domain.model.MatchingSize;

import java.util.List;

public interface SubmitReviewUseCase {

    /** Submits a new review and returns the ID of the created review. */
    Long execute(Command command);

    record Command(
            Long listingVariantId,
            Long skuId,                // nullable — which size they're reviewing
            Long orderId,
            Long authorId,
            int rating,                // 1–5
            String title,              // nullable
            String body,
            String pros,               // nullable
            String cons,               // nullable
            MatchingSize matchingSize, // nullable
            List<String> photoUrls     // deferred — silently ignored until customer upload endpoint exists
    ) {}
}
