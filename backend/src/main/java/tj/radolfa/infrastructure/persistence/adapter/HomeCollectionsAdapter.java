package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadHomeCollectionsPort;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hexagonal adapter: SQL-backed queries for homepage collection sections.
 *
 * <p>Reuses the same {@code Object[]} column layout as the grid queries
 * in {@link ListingReadAdapter}, with batch-loaded images.
 */
@Component
public class HomeCollectionsAdapter implements LoadHomeCollectionsPort {

    private final ListingVariantRepository variantRepo;

    public HomeCollectionsAdapter(ListingVariantRepository variantRepo) {
        this.variantRepo = variantRepo;
    }

    @Override
    public List<ListingVariantDto> loadFeatured(int limit) {
        return toGridDtos(variantRepo.findFeaturedGrid(PageRequest.of(0, limit)));
    }

    @Override
    public List<ListingVariantDto> loadNewArrivals(int limit) {
        return toGridDtos(variantRepo.findNewArrivalsGrid(PageRequest.of(0, limit)));
    }

    @Override
    public List<ListingVariantDto> loadOnSale(int limit) {
        return toGridDtos(variantRepo.findOnSaleGrid(PageRequest.of(0, limit)));
    }

    // ---- Shared helpers (same column layout as ListingReadAdapter) ----

    private List<ListingVariantDto> toGridDtos(List<Object[]> rows) {
        List<Long> variantIds = rows.stream()
                .map(row -> (Long) row[0])
                .toList();

        Map<Long, List<String>> imageMap = loadImageMap(variantIds);

        return rows.stream()
                .map(row -> toGridDto(row, imageMap))
                .toList();
    }

    private ListingVariantDto toGridDto(Object[] row, Map<Long, List<String>> imageMap) {
        Long id = (Long) row[0];
        return new ListingVariantDto(
                id,
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                imageMap.getOrDefault(id, List.of()),
                toBigDecimal(row[6]),
                toBigDecimal(row[7]),
                toInteger(row[8]),
                (Boolean) row[5]);
    }

    private Map<Long, List<String>> loadImageMap(List<Long> variantIds) {
        if (variantIds.isEmpty())
            return Map.of();
        return variantRepo.findImagesByVariantIds(variantIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null)
            return null;
        if (value instanceof BigDecimal bd)
            return bd;
        return new BigDecimal(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null)
            return 0;
        if (value instanceof Long l)
            return l.intValue();
        if (value instanceof Integer i)
            return i;
        return ((Number) value).intValue();
    }
}
