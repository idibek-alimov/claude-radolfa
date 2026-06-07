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
import tj.radolfa.application.ports.in.home.GetActiveFeaturedCategoriesUseCase;
import tj.radolfa.application.ports.in.home.GetActiveHomeBannersUseCase;
import tj.radolfa.application.readmodel.CollectionPageDto;
import tj.radolfa.application.readmodel.HomeSectionDto;
import tj.radolfa.infrastructure.web.dto.FeaturedCategoryPublicDto;
import tj.radolfa.infrastructure.web.dto.HomeBannerDto;

import java.util.List;

/**
 * Public homepage API — serves curated product collection sections.
 */
@RestController
@RequestMapping("/api/v1/home")
@Tag(name = "Home", description = "Homepage collection sections")
public class HomeController {

    private final GetHomeCollectionsUseCase getHomeCollectionsUseCase;
    private final GetActiveHomeBannersUseCase getActiveHomeBannersUseCase;
    private final GetActiveFeaturedCategoriesUseCase getActiveFeaturedCategoriesUseCase;
    private final TierPricingEnricher tierPricing;

    public HomeController(GetHomeCollectionsUseCase getHomeCollectionsUseCase,
                          GetActiveHomeBannersUseCase getActiveHomeBannersUseCase,
                          GetActiveFeaturedCategoriesUseCase getActiveFeaturedCategoriesUseCase,
                          TierPricingEnricher tierPricing) {
        this.getHomeCollectionsUseCase = getHomeCollectionsUseCase;
        this.getActiveHomeBannersUseCase = getActiveHomeBannersUseCase;
        this.getActiveFeaturedCategoriesUseCase = getActiveFeaturedCategoriesUseCase;
        this.tierPricing = tierPricing;
    }

    @GetMapping("/collections")
    @Operation(summary = "Homepage collections",
               description = "Returns ordered sections (Featured, New Arrivals, Deals) for the homepage")
    public ResponseEntity<List<HomeSectionDto>> collections() {
        return ResponseEntity.ok(tierPricing.enrichSections(getHomeCollectionsUseCase.getHomeSections()));
    }

    @GetMapping("/banner")
    @Operation(summary = "Active home banners",
               description = "Returns all active banners for the MAIN and WELCOME hero slots.")
    public ResponseEntity<List<HomeBannerDto>> activeBanners() {
        List<HomeBannerDto> dtos = getActiveHomeBannersUseCase.execute()
                .stream().map(HomeBannerDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/featured-categories")
    @Operation(summary = "Active featured categories",
               description = "Returns admin-curated featured-category entries, ordered by displayOrder, enriched with live category data.")
    public ResponseEntity<List<FeaturedCategoryPublicDto>> activeFeaturedCategories() {
        List<FeaturedCategoryPublicDto> dtos = getActiveFeaturedCategoriesUseCase.execute()
                .stream().map(FeaturedCategoryPublicDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/collections/{key}")
    @Operation(summary = "Paginated collection",
               description = "Returns a paginated list for a single collection (e.g. new_arrivals, on_sale, featured)")
    public ResponseEntity<CollectionPageDto> collectionByKey(
            @PathVariable String key,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "12") int limit) {

        return getHomeCollectionsUseCase.getSection(key, page, limit)
                .map(tierPricing::enrich)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
