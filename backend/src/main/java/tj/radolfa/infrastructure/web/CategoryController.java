package tj.radolfa.infrastructure.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tj.radolfa.application.ports.in.GetCategoryBlueprintUseCase;
import tj.radolfa.application.ports.in.GetCategoryBlueprintUseCase.BlueprintEntryDto;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.LoadCategoryPort.CategoryView;
import tj.radolfa.application.ports.out.LoadListingPort;
import tj.radolfa.infrastructure.web.dto.CategoryTreeDto;
import tj.radolfa.application.readmodel.ListingVariantDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final LoadCategoryPort loadCategoryPort;
    private final LoadListingPort loadListingPort;
    private final TierPricingEnricher tierPricing;
    private final GetCategoryBlueprintUseCase getBlueprintUseCase;

    public CategoryController(LoadCategoryPort loadCategoryPort,
                               LoadListingPort loadListingPort,
                               TierPricingEnricher tierPricing,
                               GetCategoryBlueprintUseCase getBlueprintUseCase) {
        this.loadCategoryPort = loadCategoryPort;
        this.loadListingPort = loadListingPort;
        this.tierPricing = tierPricing;
        this.getBlueprintUseCase = getBlueprintUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CategoryTreeDto>> getCategoryTree() {
        List<CategoryView> all = loadCategoryPort.findAll();
        List<CategoryTreeDto> tree = buildTree(all);
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/{slug}/products")
    public ResponseEntity<PageResponse<ListingVariantDto>> getProductsByCategory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit) {

        CategoryView category = loadCategoryPort.findBySlug(slug).orElse(null);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        List<Long> categoryIds = loadCategoryPort.getAllDescendantIds(category.id());
        tj.radolfa.domain.model.PageResult<ListingVariantDto> result = loadListingPort.loadByCategoryIds(categoryIds, page, limit);
        return ResponseEntity.ok(PageResponse.from(tierPricing.enrich(result)));
    }

    /**
     * GET /api/v1/categories/{id}/blueprint
     * Returns the attribute blueprint for a category (ordered by sort_order).
     * Empty list if no blueprint is configured. 404 if category does not exist.
     */
    @GetMapping("/{id}/blueprint")
    public ResponseEntity<List<BlueprintEntryDto>> getCategoryBlueprint(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(getBlueprintUseCase.getBlueprint(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
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
