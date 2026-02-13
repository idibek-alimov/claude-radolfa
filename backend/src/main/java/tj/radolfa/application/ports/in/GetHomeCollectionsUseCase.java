package tj.radolfa.application.ports.in;

import tj.radolfa.infrastructure.web.dto.HomeSectionDto;

import java.util.List;

/**
 * In-Port: serves homepage collection sections (Featured, New Arrivals, Deals).
 */
public interface GetHomeCollectionsUseCase {

    List<HomeSectionDto> getHomeSections();
}
