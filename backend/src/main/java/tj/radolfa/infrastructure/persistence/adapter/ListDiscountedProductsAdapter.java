package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.DiscountedProductFilter;
import tj.radolfa.application.ports.out.ListDiscountedProductsPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountSummary;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.DiscountedProductRow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ListDiscountedProductsAdapter implements ListDiscountedProductsPort {

    private static final Map<String, String> SORT_COLUMN_WHITELIST = Map.of(
            "skuCode",       "s.sku_code",
            "productName",   "pb.name",
            "originalPrice", "s.original_price",
            "finalPrice",    "final_price",
            "deltaPercent",  "w.amount_value",
            "stockQuantity", "s.stock_quantity"
    );

    @PersistenceContext
    private EntityManager em;

    private final LoadDiscountPort loadDiscountPort;

    public ListDiscountedProductsAdapter(LoadDiscountPort loadDiscountPort) {
        this.loadDiscountPort = loadDiscountPort;
    }

    @Override
    public Page<DiscountedProductRow> findDiscountedProducts(DiscountedProductFilter filter, Pageable pageable) {
        String whereClause = buildWhereClause(filter);
        String orderClause = buildOrderClause(pageable);

        String baseSql = """
                FROM skus s
                JOIN listing_variants lv ON lv.id = s.listing_variant_id
                JOIN product_bases pb ON pb.id = lv.product_base_id
                JOIN LATERAL (
                    SELECT d.id, d.amount_value, dt.rank,
                           d.title, d.color_hex, dt.id AS type_id, dt.name AS type_name
                    FROM discounts d
                    JOIN discount_types dt ON dt.id = d.discount_type_id
                    JOIN discount_items di ON di.discount_id = d.id
                    WHERE di.item_code = s.sku_code
                      AND d.is_disabled = FALSE
                      AND d.valid_from <= NOW()
                      AND d.valid_upto >= NOW()
                      AND d.amount_type = 'PERCENT'
                    ORDER BY dt.rank ASC, d.id ASC
                    LIMIT 1
                ) w ON TRUE
                """ + whereClause;

        String selectSql = """
                SELECT s.id AS sku_id,
                       s.sku_code,
                       s.size_label,
                       s.stock_quantity,
                       s.original_price,
                       s.original_price * (100 - w.amount_value) / 100 AS final_price,
                       w.amount_value AS delta_percent,
                       w.id AS winner_id,
                       w.amount_value AS winner_value,
                       w.title AS winner_title,
                       w.color_hex AS winner_color_hex,
                       w.rank AS winner_rank,
                       w.type_id AS winner_type_id,
                       w.type_name AS winner_type_name,
                       lv.id AS variant_id,
                       lv.product_code,
                       pb.id AS product_base_id,
                       pb.name AS product_name,
                       (SELECT lvi.image_url
                        FROM listing_variant_images lvi
                        WHERE lvi.listing_variant_id = lv.id
                        ORDER BY lvi.is_primary DESC NULLS LAST, lvi.sort_order ASC
                        LIMIT 1) AS image_url
                """ + baseSql + " " + orderClause;

        String countSql = "SELECT COUNT(*) " + baseSql;

        Query selectQuery = em.createNativeQuery(selectSql);
        Query countQuery = em.createNativeQuery(countSql);
        bindParams(filter, selectQuery, countQuery);

        selectQuery.setFirstResult((int) pageable.getOffset());
        selectQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = selectQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        if (rows.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);

        List<String> allSkuCodes = rows.stream()
                .map(r -> (String) r[1])
                .distinct()
                .toList();

        Set<Long> winnerIds = rows.stream()
                .map(r -> ((Number) r[7]).longValue())
                .collect(Collectors.toSet());

        List<Discount> allActive = loadDiscountPort.findActiveByItemCodes(allSkuCodes);
        Map<String, List<DiscountSummary>> othersBySkuCode = buildOthersMap(allActive, winnerIds, allSkuCodes);

        List<DiscountedProductRow> content = rows.stream()
                .map(r -> toRow(r, othersBySkuCode))
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    private Map<String, List<DiscountSummary>> buildOthersMap(List<Discount> allActive,
                                                               Set<Long> winnerIds,
                                                               List<String> skuCodes) {
        Map<String, List<DiscountSummary>> result = new HashMap<>();
        for (String code : skuCodes) {
            result.put(code, new ArrayList<>());
        }
        for (Discount d : allActive) {
            if (winnerIds.contains(d.id())) continue;
            DiscountSummary summary = new DiscountSummary(
                    d.id(), d.title(), d.colorHex(), d.amountValue(), d.amountType(), d.type());
            for (String code : d.itemCodes()) {
                if (result.containsKey(code)) {
                    result.get(code).add(summary);
                }
            }
        }
        return result;
    }

    private DiscountedProductRow toRow(Object[] r, Map<String, List<DiscountSummary>> othersBySkuCode) {
        Long skuId          = ((Number) r[0]).longValue();
        String skuCode      = (String) r[1];
        String sizeLabel    = (String) r[2];
        Integer stock       = ((Number) r[3]).intValue();
        BigDecimal origPrice = (BigDecimal) r[4];
        BigDecimal finalPrice = (BigDecimal) r[5];
        BigDecimal delta    = (BigDecimal) r[6];
        Long winnerId       = ((Number) r[7]).longValue();
        BigDecimal winnerVal = (BigDecimal) r[8];
        String winnerTitle  = (String) r[9];
        String winnerColorHex = (String) r[10];
        int winnerRank      = ((Number) r[11]).intValue();
        Long winnerTypeId   = ((Number) r[12]).longValue();
        String winnerTypeName = (String) r[13];
        Long variantId      = ((Number) r[14]).longValue();
        String productCode  = (String) r[15];
        Long productBaseId  = ((Number) r[16]).longValue();
        String productName  = (String) r[17];
        String imageUrl     = (String) r[18];

        // Winner is always PERCENT-type in Phase 9 (LATERAL WHERE filters amount_type='PERCENT')
        DiscountSummary winner = new DiscountSummary(
                winnerId, winnerTitle, winnerColorHex, winnerVal, AmountType.PERCENT,
                new DiscountType(winnerTypeId, winnerTypeName, winnerRank)
        );

        List<DiscountSummary> others = othersBySkuCode.getOrDefault(skuCode, List.of());

        return new DiscountedProductRow(
                skuId, skuCode, sizeLabel, stock,
                origPrice, finalPrice, delta,
                winner, others,
                productBaseId, productName, variantId, productCode, imageUrl
        );
    }

    private String buildWhereClause(DiscountedProductFilter filter) {
        List<String> conditions = new ArrayList<>();

        if (filter.search() != null && !filter.search().isBlank()) {
            conditions.add("(LOWER(pb.name) LIKE :search OR LOWER(s.sku_code) LIKE :search)");
        }
        if (filter.campaignId() != null) {
            conditions.add("EXISTS (SELECT 1 FROM discount_items di2 WHERE di2.item_code = s.sku_code AND di2.discount_id = :campaignId)");
        }
        if (filter.minDeltaPercent() != null) {
            conditions.add("w.amount_value >= :minDelta");
        }
        if (filter.maxDeltaPercent() != null) {
            conditions.add("w.amount_value <= :maxDelta");
        }

        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
    }

    private String buildOrderClause(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return "ORDER BY s.sku_code ASC";
        }
        List<String> parts = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String col = SORT_COLUMN_WHITELIST.get(order.getProperty());
            if (col == null) continue;
            parts.add(col + (order.isAscending() ? " ASC" : " DESC"));
        }
        return parts.isEmpty() ? "ORDER BY s.sku_code ASC" : "ORDER BY " + String.join(", ", parts);
    }

    private void bindParams(DiscountedProductFilter filter, Query... queries) {
        if (filter.search() != null && !filter.search().isBlank()) {
            String pattern = "%" + filter.search().toLowerCase() + "%";
            for (Query q : queries) q.setParameter("search", pattern);
        }
        if (filter.campaignId() != null) {
            for (Query q : queries) q.setParameter("campaignId", filter.campaignId());
        }
        if (filter.minDeltaPercent() != null) {
            for (Query q : queries) q.setParameter("minDelta", filter.minDeltaPercent());
        }
        if (filter.maxDeltaPercent() != null) {
            for (Query q : queries) q.setParameter("maxDelta", filter.maxDeltaPercent());
        }
    }
}
