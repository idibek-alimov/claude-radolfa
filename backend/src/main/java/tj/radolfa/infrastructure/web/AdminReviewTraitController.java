package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.review.CreateReviewTraitUseCase;
import tj.radolfa.application.ports.in.review.DeleteReviewTraitUseCase;
import tj.radolfa.application.ports.in.review.ListReviewTraitsUseCase;
import tj.radolfa.application.ports.in.review.UpdateReviewTraitUseCase;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.infrastructure.web.dto.CreateReviewTraitRequestDto;
import tj.radolfa.infrastructure.web.dto.MessageResponseDto;
import tj.radolfa.infrastructure.web.dto.ReviewTraitDto;
import tj.radolfa.infrastructure.web.dto.UpdateReviewTraitRequestDto;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for managing the global review trait bank.
 */
@RestController
@RequestMapping("/api/v1/admin/review-traits")
@Tag(name = "Admin — Review Traits", description = "CRUD for the global review trait bank. Traits are linked to categories to drive dynamic review forms.")
public class AdminReviewTraitController {

    private final CreateReviewTraitUseCase createReviewTraitUseCase;
    private final UpdateReviewTraitUseCase updateReviewTraitUseCase;
    private final DeleteReviewTraitUseCase deleteReviewTraitUseCase;
    private final ListReviewTraitsUseCase  listReviewTraitsUseCase;

    public AdminReviewTraitController(CreateReviewTraitUseCase createReviewTraitUseCase,
                                      UpdateReviewTraitUseCase updateReviewTraitUseCase,
                                      DeleteReviewTraitUseCase deleteReviewTraitUseCase,
                                      ListReviewTraitsUseCase listReviewTraitsUseCase) {
        this.createReviewTraitUseCase = createReviewTraitUseCase;
        this.updateReviewTraitUseCase = updateReviewTraitUseCase;
        this.deleteReviewTraitUseCase = deleteReviewTraitUseCase;
        this.listReviewTraitsUseCase  = listReviewTraitsUseCase;
    }

    @Operation(summary = "List all review traits",
               description = "Returns the full trait bank. MANAGER + ADMIN.")
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<ReviewTraitDto> list() {
        return listReviewTraitsUseCase.execute().stream().map(this::toDto).toList();
    }

    @Operation(summary = "Create review trait",
               description = "Adds a new trait to the global bank. Key is immutable after creation. ADMIN only.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> create(
            @Valid @RequestBody CreateReviewTraitRequestDto request) {

        Long id = createReviewTraitUseCase.execute(
                request.key(), request.labelI18n(), request.inputType());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("traitId", id));
    }

    @Operation(summary = "Update review trait",
               description = "Updates the label and/or input type of an existing trait. Key is immutable. ADMIN only.")
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewTraitRequestDto request) {

        updateReviewTraitUseCase.execute(id, request.labelI18n(), request.inputType());
        return ResponseEntity.ok(MessageResponseDto.success("Review trait updated successfully."));
    }

    @Operation(summary = "Delete review trait",
               description = "Deletes a trait. All category links are removed automatically. ADMIN only.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> delete(@PathVariable Long id) {
        deleteReviewTraitUseCase.execute(id);
        return ResponseEntity.ok(MessageResponseDto.success("Review trait deleted successfully."));
    }

    private ReviewTraitDto toDto(ReviewTrait trait) {
        return new ReviewTraitDto(
                trait.getId(),
                trait.getKey(),
                trait.getLabelI18n(),
                trait.getInputType());
    }
}
