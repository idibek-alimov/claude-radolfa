package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tj.radolfa.application.ports.in.GenericUploadImageUseCase;
import tj.radolfa.application.ports.in.review.SubmitReviewUseCase;
import tj.radolfa.application.ports.in.review.VoteReviewUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.LoadReviewVoteCountsPort;
import tj.radolfa.application.readmodel.ReviewStorefrontView;
import tj.radolfa.domain.exception.ImageProcessingException;
import tj.radolfa.domain.model.Review;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.ReviewVoteRequestDto;
import tj.radolfa.infrastructure.web.dto.SubmitReviewRequestDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reviews", description = "Customer reviews — submit, browse, and rate products")
public class ReviewController {

    private static final int MAX_PAGE_SIZE   = 50;
    private static final int MAX_PHOTO_COUNT = 5;

    private final SubmitReviewUseCase       submitReviewUseCase;
    private final VoteReviewUseCase         voteReviewUseCase;
    private final LoadReviewPort            loadReviewPort;
    private final LoadReviewVoteCountsPort  loadReviewVoteCountsPort;
    private final LoadListingVariantPort    loadListingVariantPort;
    private final GenericUploadImageUseCase genericUploadImageUseCase;

    public ReviewController(SubmitReviewUseCase submitReviewUseCase,
                            VoteReviewUseCase voteReviewUseCase,
                            LoadReviewPort loadReviewPort,
                            LoadReviewVoteCountsPort loadReviewVoteCountsPort,
                            LoadListingVariantPort loadListingVariantPort,
                            GenericUploadImageUseCase genericUploadImageUseCase) {
        this.submitReviewUseCase      = submitReviewUseCase;
        this.voteReviewUseCase        = voteReviewUseCase;
        this.loadReviewPort           = loadReviewPort;
        this.loadReviewVoteCountsPort = loadReviewVoteCountsPort;
        this.loadListingVariantPort   = loadListingVariantPort;
        this.genericUploadImageUseCase = genericUploadImageUseCase;
    }

    @PostMapping("/reviews")
    @Operation(summary = "Submit a review",
               description = "Authenticated customers who have a delivered order can submit a review for a purchased variant")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Review submitted and pending moderation"),
        @ApiResponse(responseCode = "403", description = "Product not purchased or order not delivered"),
        @ApiResponse(responseCode = "409", description = "Review already submitted for this order+variant")
    })
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

    @PostMapping(value = "/reviews/upload-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Upload review photos",
               description = "Upload up to 5 photos for a review. Returns permanent S3 URLs to pass into the review submission request.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Photos uploaded; body contains {urls}"),
        @ApiResponse(responseCode = "400", description = "No files provided or more than 5 files"),
        @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, List<String>>> uploadReviewPhotos(
            @RequestParam("files") List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (files.size() > MAX_PHOTO_COUNT) {
            return ResponseEntity.badRequest().build();
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                String url = genericUploadImageUseCase.upload(
                        file.getInputStream(),
                        file.getOriginalFilename());
                urls.add(url);
            } catch (IOException e) {
                throw new ImageProcessingException(
                        "Failed to read uploaded file: " + file.getOriginalFilename(), e);
            }
        }

        return ResponseEntity.ok(Map.of("urls", urls));
    }

    @GetMapping("/listings/{slug}/reviews")
    @Operation(summary = "List approved reviews",
               description = "Paginated approved reviews for a listing variant. Supports sort=newest|highest|lowest and hasPhotos filter.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of reviews"),
        @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    public ResponseEntity<Page<ReviewStorefrontView>> getReviews(
            @Parameter(description = "Listing variant slug") @PathVariable String slug,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page (max 50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort order: newest | highest | lowest") @RequestParam(defaultValue = "newest") String sort,
            @Parameter(description = "When true, only reviews with photos are returned") @RequestParam(defaultValue = "false") boolean hasPhotos) {

        return loadListingVariantPort.findBySlug(slug)
                .map(variant -> {
                    int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
                    Sort sortOrder = toSort(sort);

                    Page<Review> reviewPage = loadReviewPort.findApprovedByVariant(
                            variant.getId(), hasPhotos, PageRequest.of(page, effectiveSize, sortOrder));

                    List<Long> ids = reviewPage.getContent().stream().map(Review::getId).toList();
                    Map<Long, int[]> voteCounts = loadReviewVoteCountsPort.findVoteCountsByReviewIds(ids);

                    Page<ReviewStorefrontView> views = reviewPage.map(r -> {
                        int[] counts = voteCounts.getOrDefault(r.getId(), new int[]{0, 0});
                        return new ReviewStorefrontView(
                                r.getId(),
                                r.getAuthorName(),
                                r.getRating(),
                                r.getTitle(),
                                r.getBody(),
                                r.getPros(),
                                r.getCons(),
                                r.getMatchingSize(),
                                r.getPhotos(),
                                r.getSellerReply(),
                                r.getCreatedAt(),
                                counts[0],
                                counts[1]);
                    });
                    return ResponseEntity.ok(views);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reviews/{id}/vote")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Vote on a review",
               description = "Cast or update a HELPFUL / NOT_HELPFUL vote on an approved review. One vote per user per review.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Vote recorded"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<Void> voteOnReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewVoteRequestDto request,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        voteReviewUseCase.execute(new VoteReviewUseCase.Command(id, principal.userId(), request.vote()));
        return ResponseEntity.noContent().build();
    }

    private static Sort toSort(String sort) {
        return switch (sort) {
            case "highest" -> Sort.by(Sort.Direction.DESC, "rating");
            case "lowest"  -> Sort.by(Sort.Direction.ASC,  "rating");
            default        -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
