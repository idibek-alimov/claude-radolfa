package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.in.question.AskProductQuestionUseCase;
import tj.radolfa.application.ports.out.LoadProductQuestionPort;
import tj.radolfa.application.readmodel.QuestionView;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.AskQuestionRequestDto;

import java.util.Map;

@RestController
@Tag(name = "Q&A", description = "Pre-purchase product questions and answers")
public class QuestionController {

    private static final int MAX_PAGE_SIZE = 50;

    private final AskProductQuestionUseCase askProductQuestionUseCase;
    private final LoadProductQuestionPort   loadProductQuestionPort;

    public QuestionController(AskProductQuestionUseCase askProductQuestionUseCase,
                              LoadProductQuestionPort loadProductQuestionPort) {
        this.askProductQuestionUseCase = askProductQuestionUseCase;
        this.loadProductQuestionPort   = loadProductQuestionPort;
    }

    @PostMapping("/api/v1/questions")
    @Operation(summary = "Ask a question",
               description = "Authenticated users can submit a pre-purchase question on a product")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Question submitted and pending moderation"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Map<String, Long>> ask(
            @Valid @RequestBody AskQuestionRequestDto request,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        Long questionId = askProductQuestionUseCase.execute(
                request.productBaseId(),
                principal.userId(),
                request.questionText());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("questionId", questionId));
    }

    @GetMapping("/api/v1/products/{productBaseId}/questions")
    @Operation(summary = "List published questions",
               description = "Paginated published Q&A for a product, ordered by creation date descending")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of published questions")
    })
    public ResponseEntity<Page<QuestionView>> getPublishedQuestions(
            @Parameter(description = "Product base ID") @PathVariable Long productBaseId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page (max 50)") @RequestParam(defaultValue = "10") int size) {

        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<QuestionView> result = loadProductQuestionPort
                .findPublishedByProductBase(productBaseId, PageRequest.of(page, effectiveSize))
                .map(q -> new QuestionView(
                        q.getId(),
                        q.getAuthorName(),
                        q.getQuestionText(),
                        q.getAnswerText(),
                        q.getAnsweredAt(),
                        q.getCreatedAt()));

        return ResponseEntity.ok(result);
    }
}
