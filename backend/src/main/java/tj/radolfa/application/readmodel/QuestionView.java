package tj.radolfa.application.readmodel;

import java.time.Instant;

/**
 * Read model returned to the storefront for published Q&A questions.
 * Never exposes authorId — only the display name.
 */
public record QuestionView(
        Long id,
        String authorName,
        String questionText,
        String answerText,
        Instant answeredAt,
        Instant createdAt
) {}
