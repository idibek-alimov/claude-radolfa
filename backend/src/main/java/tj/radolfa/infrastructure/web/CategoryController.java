package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tj.radolfa.application.ports.in.GetCategoryBlueprintUseCase;
import tj.radolfa.application.ports.in.GetCategoryBlueprintUseCase.BlueprintEntryDto;
import tj.radolfa.application.ports.in.GetCategoryUseCase;
import tj.radolfa.application.ports.in.GetListingUseCase;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.CategoryTreeDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Category tree, product listings per category, and attribute blueprints")
public class CategoryController {

    private final GetCategoryUseCase getCategoryUseCase;
    private final GetListingUseCase getListingUseCase;
    private final TierPricingEnricher tierPricing;
    private final GetCategoryBlueprintUseCase getBlueprintUseCase;

    public CategoryController(GetCategoryUseCase getCategoryUseCase,
                              GetListingUseCase getListingUseCase,
                              TierPricingEnricher tierPricing,
                              GetCategoryBlueprintUseCase getBlueprintUseCase) {
        this.getCategoryUseCase = getCategoryUseCase;
        this.getListingUseCase = getListingUseCase;
        this.tierPricing = tierPricing;
        this.getBlueprintUseCase = getBlueprintUseCase;
    }

    @Operation(summary = "Category tree", description = "Returns the full category hierarchy as a nested tree (roots with children).")
    @GetMapping
    public ResponseEntity<List<CategoryTreeDto>> getCategoryTree() {
        List<CategoryView> all = getCategoryUseCase.findAll();
        List<CategoryTreeDto> tree = buildTree(all);
        return ResponseEntity.ok(tree);
    }

    @Operation(summary = "Products by category", description = "Paginated listing grid filtered by category slug (includes all descendant categories).")
    @GetMapping("/{slug}/products")
    public ResponseEntity<PageResponse<ListingVariantDto>> getProductsByCategory(
            @Parameter(description = "Category slug") @PathVariable String slug,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "12") int limit) {

        CategoryView category = getCategoryUseCase.findBySlug(slug).orElse(null);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        List<Long> categoryIds = getCategoryUseCase.getDescendantIds(category.id());
        PageResult<ListingVariantDto> result = getListingUseCase.getByCategoryIds(categoryIds, page, limit);
        return ResponseEntity.ok(PageResponse.from(tierPricing.enrich(result)));
    }

    /**
     * GET /api/v1/categories/{id}/blueprint
     * Returns the attribute blueprint for a category (ordered by sort_order).
     * Empty list if no blueprint is configured. 404 if category does not exist.
     */
    @Operation(summary = "Category attribute blueprint",
               description = "Returns the ordered list of expected attributes for a category. " +
                             "Use this to drive the attribute form on the frontend during product creation. " +
                             "Returns an empty list if no blueprint is configured for the category.")
    @GetMapping("/{id}/blueprint")
    public ResponseEntity<List<BlueprintEntryDto>> getCategoryBlueprint(
            @Parameter(description = "Category ID") @PathVariable Long id) {
        return ResponseEntity.ok(getBlueprintUseCase.getBlueprint(id));
    }

    private List<CategoryTreeDto> buildTree(List<CategoryView> all) {
        Map<Long, List<CategoryView>> childrenMap = new HashMap<>();
        List<CategoryView> roots = new ArrayList<>();

        for (CategoryView cv : all) {
            if (cv.parentId() == null) {
                roots.add(cv);
            } else {
                childrenMap.computeIfAbsent(cv.parentId(), k -> new ArrayList<>()).add(cv);
            }
        }

        return roots.stream().map(root -> toTreeDto(root, childrenMap)).toList();
    }

    private CategoryTreeDto toTreeDto(CategoryView view, Map<Long, List<CategoryView>> childrenMap) {
        List<CategoryView> children = childrenMap.getOrDefault(view.id(), List.of());
        List<CategoryTreeDto> childDtos = children.stream()
                .map(child -> toTreeDto(child, childrenMap))
                .toList();
        return new CategoryTreeDto(view.id(), view.name(), view.slug(), childDtos);
    }
}
