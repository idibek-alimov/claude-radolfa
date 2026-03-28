package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.in.review.SubmitReviewUseCase;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.SubmitReviewRequestDto;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final SubmitReviewUseCase submitReviewUseCase;

    public ReviewController(SubmitReviewUseCase submitReviewUseCase) {
        this.submitReviewUseCase = submitReviewUseCase;
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> submitReview(
            @Valid @RequestBody SubmitReviewRequestDto request,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        Long reviewId = submitReviewUseCase.execute(new SubmitReviewUseCase.Command(
                request.listingVariantId(),
                request.skuId(),
                request.orderId(),
                principal.userId(),
                request.rating(),
                request.title(),
                request.body(),
                request.pros(),
                request.cons(),
                request.matchingSize(),
                request.photoUrls()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("reviewId", reviewId));
    }
}
