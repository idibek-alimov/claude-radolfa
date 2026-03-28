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
import tj.radolfa.application.ports.in.review.ModerateReviewUseCase;
import tj.radolfa.application.ports.in.review.ReplyToReviewUseCase;
import tj.radolfa.application.readmodel.ReviewAdminView;
import tj.radolfa.application.services.GetPendingReviewsService;
import tj.radolfa.infrastructure.web.dto.ReplyToReviewRequestDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reviews")
public class ReviewManagementController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT     = 200;

    private final GetPendingReviewsService getPendingReviewsService;
    private final ModerateReviewUseCase    moderateReviewUseCase;
    private final ReplyToReviewUseCase     replyToReviewUseCase;

    public ReviewManagementController(GetPendingReviewsService getPendingReviewsService,
                                      ModerateReviewUseCase moderateReviewUseCase,
                                      ReplyToReviewUseCase replyToReviewUseCase) {
        this.getPendingReviewsService = getPendingReviewsService;
        this.moderateReviewUseCase    = moderateReviewUseCase;
        this.replyToReviewUseCase     = replyToReviewUseCase;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ReviewAdminView>> getPendingReviews(
            @RequestParam(defaultValue = "50") int limit) {

        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return ResponseEntity.ok(getPendingReviewsService.getPending(effectiveLimit));
    }

    @PatchMapping("/{reviewId}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long reviewId) {
        moderateReviewUseCase.approve(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reviewId}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long reviewId) {
        moderateReviewUseCase.reject(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<Void> reply(@PathVariable Long reviewId,
                                      @Valid @RequestBody ReplyToReviewRequestDto request) {
        replyToReviewUseCase.execute(reviewId, request.replyText());
        return ResponseEntity.noContent().build();
    }
}
