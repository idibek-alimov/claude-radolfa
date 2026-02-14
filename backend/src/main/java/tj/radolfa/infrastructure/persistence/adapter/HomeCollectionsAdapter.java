package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadHomeCollectionsPort;
import tj.radolfa.domain.model.PageResult;
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

    // ---- Homepage preview (limited, no pagination metadata) ----

    @Override
    public List<ListingVariantDto> loadFeatured(int limit) {
        return toGridDtos(variantRepo.findFeaturedGrid(PageRequest.of(0, limit)).getContent());
    }

    @Override
    public List<ListingVariantDto> loadNewArrivals(int limit) {
        return toGridDtos(variantRepo.findNewArrivalsGrid(PageRequest.of(0, limit)).getContent());
    }

    @Override
    public List<ListingVariantDto> loadOnSale(int limit) {
        return toGridDtos(variantRepo.findOnSaleGrid(PageRequest.of(0, limit)).getContent());
    }

    // ---- Paginated "View All" pages ----

    @Override
    public PageResult<ListingVariantDto> loadFeaturedPage(int page, int limit) {
        return toPageResult(variantRepo.findFeaturedGrid(PageRequest.of(page - 1, limit)), page);
    }

    @Override
    public PageResult<ListingVariantDto> loadNewArrivalsPage(int page, int limit) {
        return toPageResult(variantRepo.findNewArrivalsGrid(PageRequest.of(page - 1, limit)), page);
    }

    @Override
    public PageResult<ListingVariantDto> loadOnSalePage(int page, int limit) {
        return toPageResult(variantRepo.findOnSaleGrid(PageRequest.of(page - 1, limit)), page);
    }

    // ---- Shared helpers (same column layout as ListingReadAdapter) ----

    private PageResult<ListingVariantDto> toPageResult(Page<Object[]> raw, int page) {
        List<ListingVariantDto> items = toGridDtos(raw.getContent());
        return new PageResult<>(items, raw.getTotalElements(), page,
                (long) page * raw.getSize() < raw.getTotalElements());
    }

    private List<ListingVariantDto> toGridDtos(List<Object[]> rows) {
        List<Long> variantIds = rows.stream()
                .map(row -> (Long) row[0])
                .toList();

        Map<Long, List<String>> imageMap = loadImageMap(variantIds);

        return rows.stream()
                .map(row -> toGridDto(row, imageMap))
                .toList();
    }

    // Column layout: [0]=id, [1]=slug, [2]=name, [3]=categoryName, [4]=colorKey,
    //                 [5]=webDescription, [6]=topSelling, [7]=priceStart, [8]=priceEnd,
    //                 [9]=totalStock, [10]=colorHexCode, [11]=featured
    private ListingVariantDto toGridDto(Object[] row, Map<Long, List<String>> imageMap) {
        Long id = (Long) row[0];
        return new ListingVariantDto(
                id,
                (String) row[1],   // slug
                (String) row[2],   // name
                (String) row[3],   // category
                (String) row[4],   // colorKey
                (String) row[10],  // colorHexCode
                (String) row[5],   // webDescription
                imageMap.getOrDefault(id, List.of()),
                toBigDecimal(row[7]),  // priceStart
                toBigDecimal(row[8]),  // priceEnd
                toInteger(row[9]),     // totalStock
                (Boolean) row[6],      // topSelling
                (Boolean) row[11]);    // featured
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
