package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.in.question.AnswerProductQuestionUseCase;
import tj.radolfa.application.ports.in.question.ModerateProductQuestionUseCase;
import tj.radolfa.application.ports.in.review.ModerateReviewUseCase;
import tj.radolfa.application.ports.in.review.ReplyToReviewUseCase;
import tj.radolfa.application.ports.out.LoadProductQuestionPort;
import tj.radolfa.application.readmodel.QuestionView;
import tj.radolfa.application.readmodel.ReviewAdminView;
import tj.radolfa.application.services.GetPendingReviewsService;
import tj.radolfa.infrastructure.web.dto.AnswerQuestionRequestDto;
import tj.radolfa.infrastructure.web.dto.ReplyToReviewRequestDto;

import java.util.List;

@RestController
public class ReviewManagementController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT     = 200;

    private final GetPendingReviewsService       getPendingReviewsService;
    private final ModerateReviewUseCase          moderateReviewUseCase;
    private final ReplyToReviewUseCase           replyToReviewUseCase;
    private final LoadProductQuestionPort        loadProductQuestionPort;
    private final AnswerProductQuestionUseCase   answerProductQuestionUseCase;
    private final ModerateProductQuestionUseCase moderateProductQuestionUseCase;

    public ReviewManagementController(GetPendingReviewsService getPendingReviewsService,
                                      ModerateReviewUseCase moderateReviewUseCase,
                                      ReplyToReviewUseCase replyToReviewUseCase,
                                      LoadProductQuestionPort loadProductQuestionPort,
                                      AnswerProductQuestionUseCase answerProductQuestionUseCase,
                                      ModerateProductQuestionUseCase moderateProductQuestionUseCase) {
        this.getPendingReviewsService       = getPendingReviewsService;
        this.moderateReviewUseCase          = moderateReviewUseCase;
        this.replyToReviewUseCase           = replyToReviewUseCase;
        this.loadProductQuestionPort        = loadProductQuestionPort;
        this.answerProductQuestionUseCase   = answerProductQuestionUseCase;
        this.moderateProductQuestionUseCase = moderateProductQuestionUseCase;
    }

    @GetMapping("/api/v1/admin/reviews/pending")
    public ResponseEntity<List<ReviewAdminView>> getPendingReviews(
            @RequestParam(defaultValue = "50") int limit) {

        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return ResponseEntity.ok(getPendingReviewsService.getPending(effectiveLimit));
    }

    @PatchMapping("/api/v1/admin/reviews/{reviewId}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long reviewId) {
        moderateReviewUseCase.approve(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/reviews/{reviewId}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long reviewId) {
        moderateReviewUseCase.reject(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/admin/reviews/{reviewId}/reply")
    public ResponseEntity<Void> reply(@PathVariable Long reviewId,
                                      @Valid @RequestBody ReplyToReviewRequestDto request) {
        replyToReviewUseCase.execute(reviewId, request.replyText());
        return ResponseEntity.noContent().build();
    }

    // ---- Q&A Admin Endpoints ----

    @GetMapping("/api/v1/admin/questions/pending")
    public ResponseEntity<List<QuestionView>> getPendingQuestions(
            @RequestParam(defaultValue = "50") int limit) {

        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<QuestionView> result = loadProductQuestionPort.findPendingOldestFirst(effectiveLimit)
                .stream()
                .map(q -> new QuestionView(
                        q.getId(),
                        q.getAuthorName(),
                        q.getQuestionText(),
                        q.getAnswerText(),
                        q.getAnsweredAt(),
                        q.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/v1/admin/questions/{questionId}/answer")
    public ResponseEntity<Void> answerQuestion(@PathVariable Long questionId,
                                               @Valid @RequestBody AnswerQuestionRequestDto request) {
        answerProductQuestionUseCase.execute(questionId, request.answerText());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/questions/{questionId}/reject")
    public ResponseEntity<Void> rejectQuestion(@PathVariable Long questionId) {
        moderateProductQuestionUseCase.reject(questionId);
        return ResponseEntity.noContent().build();
    }
}
