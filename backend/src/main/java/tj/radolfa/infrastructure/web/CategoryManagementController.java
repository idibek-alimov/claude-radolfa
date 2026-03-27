package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.product.CreateCategoryUseCase;
import tj.radolfa.application.ports.in.product.DeleteCategoryUseCase;
import tj.radolfa.infrastructure.web.dto.CreateCategoryRequestDto;
import tj.radolfa.infrastructure.web.dto.MessageResponseDto;

import java.util.Map;

/**
 * Admin endpoints for category management.
 */
@RestController
@RequestMapping("/api/v1/admin/categories")
@Tag(name = "Admin — Category Management", description = "Admin/Manager endpoints for creating and deleting categories")
public class CategoryManagementController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    public CategoryManagementController(CreateCategoryUseCase createCategoryUseCase,
                                        DeleteCategoryUseCase deleteCategoryUseCase) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
    }

    /**
     * POST /api/v1/admin/categories
     * Create a new category. MANAGER + ADMIN.
     */
    @Operation(summary = "Create category",
               description = "Creates a new category. Optionally nest it under a parent by providing parentId. MANAGER + ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Category created; body contains {categoryId}"),
        @ApiResponse(responseCode = "400", description = "Validation failed or parent category not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient role"),
        @ApiResponse(responseCode = "409", description = "Category name already exists")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Long>> createCategory(
            @Valid @RequestBody CreateCategoryRequestDto request) {

        Long id = createCategoryUseCase.execute(request.name(), request.parentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("categoryId", id));
    }

    /**
     * DELETE /api/v1/admin/categories/{id}
     * Delete a category. ADMIN only. Fails if still in use.
     */
    @Operation(summary = "Delete category",
               description = "Deletes a category by ID. Fails with 422 if the category is still referenced by products. ADMIN only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient role"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "422", description = "Category still referenced by products")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> deleteCategory(@PathVariable Long id) {
        deleteCategoryUseCase.execute(id);
        return ResponseEntity.ok(MessageResponseDto.success("Category deleted successfully."));
    }
}
