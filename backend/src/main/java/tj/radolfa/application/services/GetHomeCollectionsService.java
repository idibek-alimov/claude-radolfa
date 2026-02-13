package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tj.radolfa.application.ports.in.GetHomeCollectionsUseCase;
import tj.radolfa.application.ports.out.LoadHomeCollectionsPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.CollectionPageDto;
import tj.radolfa.infrastructure.web.dto.HomeSectionDto;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Aggregates homepage collection sections from distinct queries.
 *
 * <p>Sections with zero items are silently omitted so the frontend
 * never renders an empty row.
 */
@Service
@Transactional(readOnly = true)
public class GetHomeCollectionsService implements GetHomeCollectionsUseCase {

    private static final int SECTION_LIMIT = 10;

    private static final Map<String, String> KEY_TO_TITLE = Map.of(
            "featured", "Featured",
            "new_arrivals", "New Arrivals",
            "on_sale", "Deals"
    );

    private final LoadHomeCollectionsPort loadHomeCollectionsPort;

    public GetHomeCollectionsService(LoadHomeCollectionsPort loadHomeCollectionsPort) {
        this.loadHomeCollectionsPort = loadHomeCollectionsPort;
    }

    @Override
    public List<HomeSectionDto> getHomeSections() {
        List<HomeSectionDto> sections = new ArrayList<>();

        addIfNotEmpty(sections, "featured", "Featured",
                loadHomeCollectionsPort.loadFeatured(SECTION_LIMIT));

        addIfNotEmpty(sections, "new_arrivals", "New Arrivals",
                loadHomeCollectionsPort.loadNewArrivals(SECTION_LIMIT));

        addIfNotEmpty(sections, "on_sale", "Deals",
                loadHomeCollectionsPort.loadOnSale(SECTION_LIMIT));

        return sections;
    }

    @Override
    public Optional<CollectionPageDto> getSection(String key, int page, int limit) {
        String title = KEY_TO_TITLE.get(key);
        if (title == null) {
            return Optional.empty();
        }

        PageResult<ListingVariantDto> result = switch (key) {
            case "featured" -> loadHomeCollectionsPort.loadFeaturedPage(page, limit);
            case "new_arrivals" -> loadHomeCollectionsPort.loadNewArrivalsPage(page, limit);
            case "on_sale" -> loadHomeCollectionsPort.loadOnSalePage(page, limit);
            default -> throw new IllegalStateException("Unknown key: " + key);
        };

        return Optional.of(new CollectionPageDto(key, title, result));
    }

    private void addIfNotEmpty(List<HomeSectionDto> sections,
                               String key, String title,
                               List<ListingVariantDto> items) {
        if (!items.isEmpty()) {
            sections.add(new HomeSectionDto(key, title, items));
        }
    }
}
