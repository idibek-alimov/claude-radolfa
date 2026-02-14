package tj.radolfa.infrastructure.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.LoadCategoryPort.CategoryView;
import tj.radolfa.application.ports.out.LoadListingPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.CategoryTreeDto;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final LoadCategoryPort loadCategoryPort;
    private final LoadListingPort loadListingPort;

    public CategoryController(LoadCategoryPort loadCategoryPort,
                               LoadListingPort loadListingPort) {
        this.loadCategoryPort = loadCategoryPort;
        this.loadListingPort = loadListingPort;
    }

    @GetMapping
    public ResponseEntity<List<CategoryTreeDto>> getCategoryTree() {
        List<CategoryView> all = loadCategoryPort.findAll();
        List<CategoryTreeDto> tree = buildTree(all);
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/{slug}/products")
    public ResponseEntity<PageResult<ListingVariantDto>> getProductsByCategory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit) {

        CategoryView category = loadCategoryPort.findBySlug(slug).orElse(null);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        List<Long> categoryIds = loadCategoryPort.getAllDescendantIds(category.id());
        PageResult<ListingVariantDto> result = loadListingPort.loadByCategoryIds(categoryIds, page, limit);
        return ResponseEntity.ok(result);
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
