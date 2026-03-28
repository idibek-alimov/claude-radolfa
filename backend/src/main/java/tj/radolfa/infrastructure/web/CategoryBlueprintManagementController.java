package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tj.radolfa.application.ports.in.CreateCategoryBlueprintUseCase;
import tj.radolfa.application.ports.in.CreateCategoryBlueprintUseCase.Command;
import tj.radolfa.application.ports.in.DeleteCategoryBlueprintUseCase;
import tj.radolfa.domain.model.AttributeType;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories/{categoryId}/blueprint")
@Tag(name = "Blueprint Management", description = "ADMIN: manage category attribute blueprint entries")
@PreAuthorize("hasRole('ADMIN')")
public class CategoryBlueprintManagementController {

    private final CreateCategoryBlueprintUseCase createBlueprintUseCase;
    private final DeleteCategoryBlueprintUseCase deleteBlueprintUseCase;

    public CategoryBlueprintManagementController(CreateCategoryBlueprintUseCase createBlueprintUseCase,
                                                 DeleteCategoryBlueprintUseCase deleteBlueprintUseCase) {
        this.createBlueprintUseCase = createBlueprintUseCase;
        this.deleteBlueprintUseCase = deleteBlueprintUseCase;
    }

    record CreateBlueprintRequest(
            @NotBlank @Size(max = 128) String attributeKey,
            @NotNull AttributeType type,
            @Size(max = 64) String unitName,
            List<@NotBlank @Size(max = 256) String> allowedValues,
            boolean required,
            int sortOrder
    ) {}

    @Operation(summary = "Create blueprint entry",
               description = "Adds an attribute blueprint entry to a category. ADMIN only.")
    @ApiResponse(responseCode = "201", description = "Blueprint entry created")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @PostMapping
    public ResponseEntity<Void> createBlueprint(
            @Parameter(description = "Category ID") @PathVariable Long categoryId,
            @Valid @RequestBody CreateBlueprintRequest request) {

        Long id = createBlueprintUseCase.execute(new Command(
                categoryId,
                request.attributeKey(),
                request.type(),
                request.unitName(),
                request.allowedValues() != null ? request.allowedValues() : List.of(),
                request.required(),
                request.sortOrder()));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Delete blueprint entry",
               description = "Removes a blueprint entry from a category. ADMIN only.")
    @ApiResponse(responseCode = "204", description = "Blueprint entry deleted")
    @ApiResponse(responseCode = "404", description = "Blueprint entry not found for this category")
    @DeleteMapping("/{blueprintId}")
    public ResponseEntity<Void> deleteBlueprint(
            @Parameter(description = "Category ID") @PathVariable Long categoryId,
            @Parameter(description = "Blueprint entry ID") @PathVariable Long blueprintId) {

        deleteBlueprintUseCase.execute(categoryId, blueprintId);
        return ResponseEntity.noContent().build();
    }
}
