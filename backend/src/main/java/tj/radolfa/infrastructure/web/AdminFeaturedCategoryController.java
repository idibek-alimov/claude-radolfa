package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.home.ManageFeaturedCategoryUseCase;
import tj.radolfa.application.ports.out.LoadFeaturedCategoryPort;
import tj.radolfa.domain.model.FeaturedCategory;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.FeaturedCategoryDto;
import tj.radolfa.infrastructure.web.dto.FeaturedCategoryRequestDto;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/featured-categories")
@Tag(name = "Admin — Featured Categories", description = "MANAGER+ADMIN endpoints for curating the homepage Featured Category spotlight")
public class AdminFeaturedCategoryController {

    private final ManageFeaturedCategoryUseCase manageFeaturedCategoryUseCase;
    private final LoadFeaturedCategoryPort loadFeaturedCategoryPort;

    public AdminFeaturedCategoryController(ManageFeaturedCategoryUseCase manageFeaturedCategoryUseCase,
                                            LoadFeaturedCategoryPort loadFeaturedCategoryPort) {
        this.manageFeaturedCategoryUseCase = manageFeaturedCategoryUseCase;
        this.loadFeaturedCategoryPort = loadFeaturedCategoryPort;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "List featured categories (paginated, searchable by title/subtitle)",
               description = "Returns all entries (active and inactive). MANAGER + ADMIN.")
    public ResponseEntity<PageResponse<FeaturedCategoryDto>> list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "displayOrder,asc") String sort) {
        PageResult<FeaturedCategory> result = loadFeaturedCategoryPort.findAllPaged(page, size, search, sort);
        List<FeaturedCategoryDto> dtos = result.content().stream().map(FeaturedCategoryDto::from).toList();
        return ResponseEntity.ok(PageResponse.from(
                new PageResult<>(dtos, result.totalElements(), result.number(), result.size(), result.last())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Create featured category", description = "Curates a category to feature on the homepage. MANAGER + ADMIN.")
    public ResponseEntity<FeaturedCategoryDto> create(@RequestBody @Valid FeaturedCategoryRequestDto request) {
        var featuredCategory = manageFeaturedCategoryUseCase.create(request.toCommand());
        return ResponseEntity
                .created(URI.create("/api/v1/admin/featured-categories/" + featuredCategory.id()))
                .body(FeaturedCategoryDto.from(featuredCategory));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Update featured category", description = "Replaces an entry by ID. MANAGER + ADMIN.")
    public ResponseEntity<FeaturedCategoryDto> update(@PathVariable Long id,
                                                       @RequestBody @Valid FeaturedCategoryRequestDto request) {
        var featuredCategory = manageFeaturedCategoryUseCase.update(id, request.toCommand());
        return ResponseEntity.ok(FeaturedCategoryDto.from(featuredCategory));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Delete featured category", description = "Removes an entry by ID. MANAGER + ADMIN.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        manageFeaturedCategoryUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
