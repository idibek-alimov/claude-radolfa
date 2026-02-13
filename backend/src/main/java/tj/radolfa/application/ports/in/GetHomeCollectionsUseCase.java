package tj.radolfa.application.ports.in;

import tj.radolfa.infrastructure.web.dto.CollectionPageDto;
import tj.radolfa.infrastructure.web.dto.HomeSectionDto;

import java.util.List;
import java.util.Optional;

/**
 * In-Port: serves homepage collection sections (Featured, New Arrivals, Deals).
 */
public interface GetHomeCollectionsUseCase {

    List<HomeSectionDto> getHomeSections();

    Optional<CollectionPageDto> getSection(String key, int page, int limit);
}
