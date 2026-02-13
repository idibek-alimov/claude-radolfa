package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tj.radolfa.application.ports.in.GetHomeCollectionsUseCase;
import tj.radolfa.infrastructure.web.dto.CollectionPageDto;
import tj.radolfa.infrastructure.web.dto.HomeSectionDto;

import java.util.List;

/**
 * Public homepage API — serves curated product collection sections.
 */
@RestController
@RequestMapping("/api/v1/home")
@Tag(name = "Home", description = "Homepage collection sections")
public class HomeController {

    private final GetHomeCollectionsUseCase getHomeCollectionsUseCase;

    public HomeController(GetHomeCollectionsUseCase getHomeCollectionsUseCase) {
        this.getHomeCollectionsUseCase = getHomeCollectionsUseCase;
    }

    @GetMapping("/collections")
    @Operation(summary = "Homepage collections",
               description = "Returns ordered sections (Featured, New Arrivals, Deals) for the homepage")
    public ResponseEntity<List<HomeSectionDto>> collections() {
        return ResponseEntity.ok(getHomeCollectionsUseCase.getHomeSections());
    }

    @GetMapping("/collections/{key}")
    @Operation(summary = "Paginated collection",
               description = "Returns a paginated list for a single collection (e.g. new_arrivals, on_sale, featured)")
    public ResponseEntity<CollectionPageDto> collectionByKey(
            @PathVariable String key,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "12") int limit) {

        return getHomeCollectionsUseCase.getSection(key, page, limit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
