package tj.radolfa.application.readmodel;

import tj.radolfa.domain.model.MatchingSize;
import tj.radolfa.domain.model.ReviewStatus;

import java.time.Instant;
import java.util.List;

/**
 * Read model returned to admins for the pending review queue.
 */
public record ReviewAdminView(
        Long id,
        Long listingVariantId,
        String variantSlug,
        String authorName,
        int rating,
        String title,
        String body,
        String pros,
        String cons,
        MatchingSize matchingSize,
        List<String> photoUrls,
        ReviewStatus status,
        String sellerReply,
        Instant createdAt
) {}
