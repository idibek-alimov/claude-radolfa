package tj.radolfa.infrastructure.web;

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
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> deleteCategory(@PathVariable Long id) {
        deleteCategoryUseCase.execute(id);
        return ResponseEntity.ok(MessageResponseDto.success("Category deleted successfully."));
    }
}
