package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tj.radolfa.application.ports.in.GetHomeCollectionsUseCase;
import tj.radolfa.application.ports.out.LoadHomeCollectionsPort;
import tj.radolfa.infrastructure.web.dto.HomeSectionDto;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.util.ArrayList;
import java.util.List;

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

    private void addIfNotEmpty(List<HomeSectionDto> sections,
                               String key, String title,
                               List<ListingVariantDto> items) {
        if (!items.isEmpty()) {
            sections.add(new HomeSectionDto(key, title, items));
        }
    }
}
