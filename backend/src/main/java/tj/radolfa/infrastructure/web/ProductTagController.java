package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.AssignVariantTagsUseCase;
import tj.radolfa.application.ports.in.CreateProductTagUseCase;
import tj.radolfa.application.ports.in.DeleteProductTagUseCase;
import tj.radolfa.application.ports.in.ListAllTagsUseCase;
import tj.radolfa.application.ports.in.UpdateProductTagUseCase;
import tj.radolfa.domain.model.ProductTag;
import tj.radolfa.application.readmodel.ListingVariantDto.TagView;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Tags", description = "Product tag management")
public class ProductTagController {

    private final ListAllTagsUseCase listAllTagsUseCase;
    private final CreateProductTagUseCase createProductTagUseCase;
    private final UpdateProductTagUseCase updateProductTagUseCase;
    private final DeleteProductTagUseCase deleteProductTagUseCase;
    private final AssignVariantTagsUseCase assignVariantTagsUseCase;

    public ProductTagController(ListAllTagsUseCase listAllTagsUseCase,
                                CreateProductTagUseCase createProductTagUseCase,
                                UpdateProductTagUseCase updateProductTagUseCase,
                                DeleteProductTagUseCase deleteProductTagUseCase,
                                AssignVariantTagsUseCase assignVariantTagsUseCase) {
        this.listAllTagsUseCase = listAllTagsUseCase;
        this.createProductTagUseCase = createProductTagUseCase;
        this.updateProductTagUseCase = updateProductTagUseCase;
        this.deleteProductTagUseCase = deleteProductTagUseCase;
        this.assignVariantTagsUseCase = assignVariantTagsUseCase;
    }

    @GetMapping("/api/v1/tags")
    @Operation(summary = "List all tags", description = "Returns all product tags available for assignment.")
    @ApiResponse(responseCode = "200", description = "Tag list returned")
    public ResponseEntity<List<TagView>> listAll() {
        return ResponseEntity.ok(
                listAllTagsUseCase.execute().stream()
                        .map(t -> new TagView(t.id(), t.name(), t.colorHex()))
                        .toList()
        );
    }

    @PostMapping("/api/v1/admin/tags")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a tag", description = "Creates a new product tag. ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tag created; body contains {id, name, colorHex}"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "409", description = "Tag name already exists")
    })
    public ResponseEntity<Map<String, Object>> createTag(
            @Valid @RequestBody CreateTagRequest request) {
        Long id = createProductTagUseCase.execute(request.name(), request.colorHex());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", id, "name", request.name(), "colorHex", request.colorHex()));
    }

    @PatchMapping("/api/v1/admin/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a tag", description = "Rename or recolor a tag. ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tag updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Tag not found"),
            @ApiResponse(responseCode = "409", description = "Tag name already exists")
    })
    public ResponseEntity<Map<String, Object>> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody CreateTagRequest request) {
        ProductTag updated = updateProductTagUseCase.execute(id, request.name(), request.colorHex());
        return ResponseEntity.ok(Map.of(
                "id", updated.id(),
                "name", updated.name(),
                "colorHex", updated.colorHex()));
    }

    @DeleteMapping("/api/v1/admin/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a tag", description = "Deletes a tag. Fails with 409 if any variant still references it.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tag deleted"),
            @ApiResponse(responseCode = "404", description = "Tag not found"),
            @ApiResponse(responseCode = "409", description = "Tag is still referenced by one or more variants")
    })
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        deleteProductTagUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/admin/variants/{variantId}/tags")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Assign tags to a variant",
            description = "Replaces all current tags on the variant with the provided list. Pass an empty list to remove all tags. MANAGER or ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tags updated"),
            @ApiResponse(responseCode = "400", description = "One or more tag IDs not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Variant not found")
    })
    public ResponseEntity<Void> assignTags(
            @PathVariable Long variantId,
            @Valid @RequestBody AssignTagsRequest request) {
        assignVariantTagsUseCase.execute(variantId, request.tagIds());
        return ResponseEntity.ok().build();
    }

    public record CreateTagRequest(
            @NotBlank(message = "Tag name is required")
            @Size(max = 64, message = "Tag name must not exceed 64 characters")
            String name,
            @NotBlank(message = "Color hex is required")
            @Pattern(regexp = "^[0-9A-Fa-f]{6}$", message = "colorHex must be a 6-character hex string")
            String colorHex) {
    }

    public record AssignTagsRequest(List<Long> tagIds) {
    }
}
