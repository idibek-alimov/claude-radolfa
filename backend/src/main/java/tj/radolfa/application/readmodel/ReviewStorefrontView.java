package tj.radolfa.application.readmodel;

import tj.radolfa.domain.model.MatchingSize;

import java.time.Instant;
import java.util.List;

/**
 * Public-safe review read model returned by the storefront endpoint.
 * Never exposes orderId, authorId, or moderation status.
 */
public record ReviewStorefrontView(
        Long id,
        String authorName,
        int rating,
        String title,
        String body,
        String pros,
        String cons,
        MatchingSize matchingSize,
        List<String> photoUrls,
        String sellerReply,
        Instant createdAt
) {}
