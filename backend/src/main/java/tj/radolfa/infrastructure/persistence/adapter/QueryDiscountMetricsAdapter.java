package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.QueryDiscountMetricsPort;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.DailyMetric;
import tj.radolfa.domain.model.DiscountMetrics;
import tj.radolfa.domain.model.DiscountSummary;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.TopCampaignRow;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class QueryDiscountMetricsAdapter implements QueryDiscountMetricsPort {

    private static final Map<String, String> ORDER_WHITELIST = Map.of(
            "revenue", "SUM(da.discount_amount)",
            "units",   "SUM(da.quantity)"
    );

    @PersistenceContext
    private EntityManager em;

    @Override
    public DiscountMetrics findMetrics(Long discountId, LocalDate from, LocalDate to) {
        String kpiSql = """
                SELECT COUNT(DISTINCT da.order_id),
                       COALESCE(SUM(da.quantity), 0),
                       COALESCE(SUM(da.discount_amount), 0.00)
                FROM discount_application da
                WHERE da.discount_id = :id
                  AND da.applied_at::date BETWEEN :from AND :to
                """;

        Query kpiQuery = em.createNativeQuery(kpiSql);
        kpiQuery.setParameter("id", discountId);
        kpiQuery.setParameter("from", from);
        kpiQuery.setParameter("to", to);

        Object[] kpi = (Object[]) kpiQuery.getSingleResult();
        long ordersUsing  = ((Number) kpi[0]).longValue();
        long unitsMoved   = ((Number) kpi[1]).longValue();
        BigDecimal uplift = (BigDecimal) kpi[2];
        BigDecimal avg    = ordersUsing == 0
                ? BigDecimal.ZERO
                : uplift.divide(BigDecimal.valueOf(ordersUsing), 2, java.math.RoundingMode.HALF_UP);

        String dailySql = """
                WITH series AS (
                  SELECT generate_series(:from::date, :to::date, '1 day'::interval)::date AS day
                ), daily AS (
                  SELECT da.applied_at::date AS day,
                         COUNT(DISTINCT da.order_id) AS orders,
                         SUM(da.quantity)            AS units,
                         SUM(da.discount_amount)     AS uplift
                  FROM discount_application da
                  WHERE da.discount_id = :id
                    AND da.applied_at::date BETWEEN :from AND :to
                  GROUP BY da.applied_at::date
                )
                SELECT s.day,
                       COALESCE(d.orders, 0),
                       COALESCE(d.units, 0),
                       COALESCE(d.uplift, 0.00)
                FROM series s
                LEFT JOIN daily d ON s.day = d.day
                ORDER BY s.day
                """;

        Query dailyQuery = em.createNativeQuery(dailySql);
        dailyQuery.setParameter("id", discountId);
        dailyQuery.setParameter("from", from);
        dailyQuery.setParameter("to", to);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dailyQuery.getResultList();
        List<DailyMetric> series = rows.stream()
                .map(r -> new DailyMetric(
                        ((Date) r[0]).toLocalDate(),
                        ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue(),
                        (BigDecimal) r[3]
                ))
                .toList();

        return new DiscountMetrics(ordersUsing, unitsMoved, uplift, avg, from, to, series);
    }

    @Override
    public List<TopCampaignRow> findTop(String by, LocalDate from, LocalDate to, int limit) {
        String orderExpr = ORDER_WHITELIST.getOrDefault(by, "SUM(da.discount_amount)");

        String sql = """
                SELECT d.id,
                       d.title,
                       d.color_hex,
                       d.amount_value,
                       d.amount_type,
                       dt.id   AS type_id,
                       dt.name AS type_name,
                       dt.rank AS type_rank,
                       COUNT(DISTINCT da.order_id) AS orders_using,
                       COALESCE(SUM(da.quantity), 0)        AS units_moved,
                       COALESCE(SUM(da.discount_amount), 0.00) AS revenue_uplift
                FROM discounts d
                JOIN discount_types dt ON dt.id = d.discount_type_id
                LEFT JOIN discount_application da ON da.discount_id = d.id
                  AND da.applied_at::date BETWEEN :from AND :to
                GROUP BY d.id, d.title, d.color_hex, d.amount_value, d.amount_type,
                         dt.id, dt.name, dt.rank
                ORDER BY """ + orderExpr + " DESC, d.id ASC LIMIT :limit";

        Query query = em.createNativeQuery(sql);
        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<TopCampaignRow> result = new ArrayList<>();
        for (Object[] r : rows) {
            Long id            = ((Number) r[0]).longValue();
            String title       = (String) r[1];
            String colorHex    = (String) r[2];
            BigDecimal dv      = (BigDecimal) r[3];
            AmountType amt     = AmountType.valueOf((String) r[4]);
            Long typeId        = ((Number) r[5]).longValue();
            String typeName    = (String) r[6];
            int typeRank       = ((Number) r[7]).intValue();
            long orders        = ((Number) r[8]).longValue();
            long units         = ((Number) r[9]).longValue();
            BigDecimal revenue = (BigDecimal) r[10];
            DiscountSummary summary = new DiscountSummary(
                    id, title, colorHex, dv, amt, new DiscountType(typeId, typeName, typeRank));
            result.add(new TopCampaignRow(summary, orders, units, revenue));
        }
        return result;
    }
}
