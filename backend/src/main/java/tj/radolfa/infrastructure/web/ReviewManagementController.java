package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.in.question.AnswerProductQuestionUseCase;
import tj.radolfa.application.ports.in.question.GetPendingQuestionsUseCase;
import tj.radolfa.application.ports.in.question.ModerateProductQuestionUseCase;
import tj.radolfa.application.ports.in.review.GetPendingReviewsUseCase;
import tj.radolfa.application.ports.in.review.ModerateReviewUseCase;
import tj.radolfa.application.ports.in.review.ReplyToReviewUseCase;
import tj.radolfa.application.readmodel.QuestionAdminView;
import tj.radolfa.application.readmodel.ReviewAdminView;
import tj.radolfa.infrastructure.web.dto.AnswerQuestionRequestDto;
import tj.radolfa.infrastructure.web.dto.ReplyToReviewRequestDto;

import java.util.List;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ReviewManagementController {

    private static final int MAX_LIMIT = 200;

    private final GetPendingReviewsUseCase       getPendingReviewsUseCase;
    private final ModerateReviewUseCase          moderateReviewUseCase;
    private final ReplyToReviewUseCase           replyToReviewUseCase;
    private final GetPendingQuestionsUseCase     getPendingQuestionsUseCase;
    private final AnswerProductQuestionUseCase   answerProductQuestionUseCase;
    private final ModerateProductQuestionUseCase moderateProductQuestionUseCase;

    public ReviewManagementController(GetPendingReviewsUseCase getPendingReviewsUseCase,
                                      ModerateReviewUseCase moderateReviewUseCase,
                                      ReplyToReviewUseCase replyToReviewUseCase,
                                      GetPendingQuestionsUseCase getPendingQuestionsUseCase,
                                      AnswerProductQuestionUseCase answerProductQuestionUseCase,
                                      ModerateProductQuestionUseCase moderateProductQuestionUseCase) {
        this.getPendingReviewsUseCase       = getPendingReviewsUseCase;
        this.moderateReviewUseCase          = moderateReviewUseCase;
        this.replyToReviewUseCase           = replyToReviewUseCase;
        this.getPendingQuestionsUseCase     = getPendingQuestionsUseCase;
        this.answerProductQuestionUseCase   = answerProductQuestionUseCase;
        this.moderateProductQuestionUseCase = moderateProductQuestionUseCase;
    }

    // ---- Review Moderation ----

    @GetMapping("/api/v1/admin/reviews/pending")
    @Tag(name = "Review Management")
    @Operation(summary = "Pending review queue", description = "Returns pending reviews oldest-first for moderation")
    @ApiResponse(responseCode = "200", description = "List of pending reviews")
    public ResponseEntity<List<ReviewAdminView>> getPendingReviews(
            @RequestParam(defaultValue = "50") int limit) {

        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return ResponseEntity.ok(getPendingReviewsUseCase.getPending(effectiveLimit));
    }

    @PatchMapping("/api/v1/admin/reviews/{reviewId}/approve")
    @Tag(name = "Review Management")
    @Operation(summary = "Approve a review", description = "Approves review and triggers rating recalculation")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Review approved"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<Void> approve(@PathVariable Long reviewId) {
        moderateReviewUseCase.approve(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/reviews/{reviewId}/reject")
    @Tag(name = "Review Management")
    @Operation(summary = "Reject a review")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Review rejected"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<Void> reject(@PathVariable Long reviewId) {
        moderateReviewUseCase.reject(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/admin/reviews/{reviewId}/reply")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Tag(name = "Review Management")
    @Operation(summary = "Post seller reply", description = "Adds a public seller reply to an approved review")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reply posted"),
        @ApiResponse(responseCode = "404", description = "Review not found"),
        @ApiResponse(responseCode = "400", description = "Review is not in APPROVED status")
    })
    public ResponseEntity<Void> reply(@PathVariable Long reviewId,
                                      @Valid @RequestBody ReplyToReviewRequestDto request) {
        replyToReviewUseCase.execute(reviewId, request.replyText());
        return ResponseEntity.noContent().build();
    }

    // ---- Q&A Admin Endpoints ----

    @GetMapping("/api/v1/admin/questions/pending")
    @Tag(name = "Q&A Management")
    @Operation(summary = "Pending question queue", description = "Returns pending questions oldest-first for moderation, enriched with product context")
    @ApiResponse(responseCode = "200", description = "List of pending questions with product context")
    public ResponseEntity<List<QuestionAdminView>> getPendingQuestions(
            @RequestParam(defaultValue = "50") int limit) {

        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return ResponseEntity.ok(getPendingQuestionsUseCase.getPending(effectiveLimit));
    }

    @PostMapping("/api/v1/admin/questions/{questionId}/answer")
    @Tag(name = "Q&A Management")
    @Operation(summary = "Answer a question", description = "Posts an answer and publishes the question to the storefront")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Question answered and published"),
        @ApiResponse(responseCode = "404", description = "Question not found")
    })
    public ResponseEntity<Void> answerQuestion(@PathVariable Long questionId,
                                               @Valid @RequestBody AnswerQuestionRequestDto request) {
        answerProductQuestionUseCase.execute(questionId, request.answerText());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/questions/{questionId}/reject")
    @Tag(name = "Q&A Management")
    @Operation(summary = "Reject a question")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Question rejected"),
        @ApiResponse(responseCode = "404", description = "Question not found")
    })
    public ResponseEntity<Void> rejectQuestion(@PathVariable Long questionId) {
        moderateProductQuestionUseCase.reject(questionId);
        return ResponseEntity.noContent().build();
    }
}
