package tj.radolfa.application.readmodel;

import tj.radolfa.domain.model.QuestionStatus;

import java.time.Instant;

/**
 * Admin-only read model for the Q&A moderation queue.
 * Includes product context (name, thumbnail, color) enriched via JOIN.
 */
public record QuestionAdminView(
        Long           id,
        String         authorName,
        String         questionText,
        String         answerText,
        Instant        answeredAt,
        Instant        createdAt,

        Long           productBaseId,
        String         productName,
        String         productSlug,      // slug of first listing variant — admin link target
        String         thumbnailUrl,     // first image of first variant (150×150)

        Long           listingVariantId, // nullable — the variant whose color is shown
        String         colorName,        // nullable — from asked variant or first variant
        String         colorHex,         // nullable — from asked variant or first variant

        QuestionStatus status
) {}
