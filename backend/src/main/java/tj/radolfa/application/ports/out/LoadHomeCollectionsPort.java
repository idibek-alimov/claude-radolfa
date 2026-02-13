package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.util.List;

/**
 * Out-Port: SQL queries for homepage collection sections.
 */
public interface LoadHomeCollectionsPort {

    List<ListingVariantDto> loadFeatured(int limit);

    List<ListingVariantDto> loadNewArrivals(int limit);

    List<ListingVariantDto> loadOnSale(int limit);

    PageResult<ListingVariantDto> loadFeaturedPage(int page, int limit);

    PageResult<ListingVariantDto> loadNewArrivalsPage(int page, int limit);

    PageResult<ListingVariantDto> loadOnSalePage(int page, int limit);
}
