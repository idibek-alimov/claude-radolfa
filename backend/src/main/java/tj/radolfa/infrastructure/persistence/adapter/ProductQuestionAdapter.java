package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadProductQuestionPort;
import tj.radolfa.application.ports.out.SaveProductQuestionPort;
import tj.radolfa.application.readmodel.QuestionAdminView;
import tj.radolfa.domain.model.ProductQuestion;
import tj.radolfa.domain.model.QuestionStatus;
import tj.radolfa.infrastructure.persistence.entity.ProductQuestionEntity;
import tj.radolfa.infrastructure.persistence.mappers.ProductQuestionMapper;
import tj.radolfa.infrastructure.persistence.repository.ProductQuestionRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class ProductQuestionAdapter implements LoadProductQuestionPort, SaveProductQuestionPort {

    // language=SQL
    private static final String BASE_SELECT = """
            SELECT
                pq.id,
                pq.author_name,
                pq.question_text,
                pq.answer_text,
                pq.answered_at,
                pq.created_at,
                pb.id                                                 AS product_base_id,
                pb.name                                               AS product_name,
                COALESCE(lv_asked.id, lv_first.id)                   AS listing_variant_id,
                lv_first.slug                                         AS product_slug,
                img.image_url                                         AS thumbnail_url,
                COALESCE(c_asked.display_name, c_first.display_name) AS color_name,
                COALESCE(c_asked.hex_code,     c_first.hex_code)     AS color_hex,
                pq.status
            FROM product_questions pq
            JOIN product_bases pb ON pb.id = pq.product_base_id
            LEFT JOIN LATERAL (
                SELECT lv2.id, lv2.slug, lv2.color_id
                FROM listing_variants lv2
                WHERE lv2.product_base_id = pb.id
                ORDER BY lv2.id ASC
                LIMIT 1
            ) lv_first ON true
            LEFT JOIN colors c_first    ON c_first.id  = lv_first.color_id
            LEFT JOIN listing_variants lv_asked ON lv_asked.id = pq.listing_variant_id
            LEFT JOIN colors c_asked    ON c_asked.id  = lv_asked.color_id
            LEFT JOIN LATERAL (
                SELECT lvi.image_url
                FROM listing_variant_images lvi
                WHERE lvi.listing_variant_id = lv_first.id
                ORDER BY lvi.sort_order ASC
                LIMIT 1
            ) img ON true
            """;

    // language=SQL
    private static final String WHERE_CLAUSE = """
            WHERE pq.status = :status
              AND (:search = '' OR pq.question_text ILIKE CONCAT('%%', :search, '%%')
                                OR pb.name          ILIKE CONCAT('%%', :search, '%%'))
              AND (CAST(:dateFrom AS TIMESTAMPTZ) IS NULL OR pq.created_at >= :dateFrom)
              AND (CAST(:dateTo   AS TIMESTAMPTZ) IS NULL OR pq.created_at <= :dateTo)
            """;

    // language=SQL
    private static final String COUNT_SQL = """
            SELECT COUNT(*)
            FROM product_questions pq
            JOIN product_bases pb ON pb.id = pq.product_base_id
            """ + WHERE_CLAUSE;

    private final ProductQuestionRepository repository;
    private final ProductQuestionMapper     mapper;
    private final EntityManager             entityManager;

    public ProductQuestionAdapter(ProductQuestionRepository repository,
                                  ProductQuestionMapper mapper,
                                  EntityManager entityManager) {
        this.repository    = repository;
        this.mapper        = mapper;
        this.entityManager = entityManager;
    }

    // ---- LoadProductQuestionPort ---------------------------------------

    @Override
    public Optional<ProductQuestion> findById(Long id) {
        return repository.findById(id).map(mapper::toProductQuestion);
    }

    @Override
    public Page<ProductQuestion> findPublishedByProductBase(Long productBaseId, Pageable pageable) {
        return repository
                .findByProductBaseIdAndStatus(productBaseId, QuestionStatus.PUBLISHED, pageable)
                .map(mapper::toProductQuestion);
    }

    @Override
    public Page<QuestionAdminView> findAdminQuestions(QuestionStatus status,
                                                       String search,
                                                       Instant dateFrom,
                                                       Instant dateTo,
                                                       int page,
                                                       int size,
                                                       String sortBy,
                                                       String sortDir) {
        String orderBy = resolveOrderBy(sortBy, sortDir);
        String dataSql = BASE_SELECT + WHERE_CLAUSE + "ORDER BY " + orderBy + "\nLIMIT :limit OFFSET :offset";

        Query dataQuery = entityManager.createNativeQuery(dataSql);
        bindWhereParams(dataQuery, status, search, dateFrom, dateTo);
        dataQuery.setParameter("limit",  size);
        dataQuery.setParameter("offset", (long) (page - 1) * size);

        Query cntQuery = entityManager.createNativeQuery(COUNT_SQL);
        bindWhereParams(cntQuery, status, search, dateFrom, dateTo);

        @SuppressWarnings("unchecked")
        List<Object[]> rows  = dataQuery.getResultList();
        long           total = ((Number) cntQuery.getSingleResult()).longValue();

        List<QuestionAdminView> content = rows.stream().map(this::mapRow).toList();
        return new PageImpl<>(content, PageRequest.of(page - 1, size), total);
    }

    // ---- SaveProductQuestionPort ---------------------------------------

    @Override
    public ProductQuestion save(ProductQuestion question) {
        ProductQuestionEntity entity;

        if (question.getId() != null) {
            entity = repository.findById(question.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "ProductQuestion not found: " + question.getId()));
            entity.setStatus(question.getStatus());
            entity.setAnswerText(question.getAnswerText());
            entity.setAnsweredAt(question.getAnsweredAt());
        } else {
            entity = mapper.toEntity(question);
        }

        return mapper.toProductQuestion(repository.save(entity));
    }

    // ---- Helpers -------------------------------------------------------

    private static void bindWhereParams(Query q,
                                         QuestionStatus status,
                                         String search,
                                         Instant dateFrom,
                                         Instant dateTo) {
        q.setParameter("status",   status.name());
        q.setParameter("search",   (search != null && !search.isBlank()) ? search.trim() : "");
        q.setParameter("dateFrom", dateFrom != null ? Timestamp.from(dateFrom) : null);
        q.setParameter("dateTo",   dateTo   != null ? Timestamp.from(dateTo)   : null);
    }

    /**
     * Resolves the ORDER BY clause from validated, non-user-interpolated column names.
     * Safe: sortBy is matched against a fixed set; sortDir is forced to ASC or DESC.
     */
    private static String resolveOrderBy(String sortBy, String sortDir) {
        String col = "answeredAt".equalsIgnoreCase(sortBy) ? "pq.answered_at" : "pq.created_at";
        String dir = "DESC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        return col + " " + dir;
    }

    /** SQL column order: 0=id, 1=author_name, 2=question_text, 3=answer_text,
     *  4=answered_at, 5=created_at, 6=product_base_id, 7=product_name,
     *  8=listing_variant_id, 9=product_slug, 10=thumbnail_url,
     *  11=color_name, 12=color_hex, 13=status */
    private QuestionAdminView mapRow(Object[] row) {
        return new QuestionAdminView(
                ((Number) row[0]).longValue(),
                (String)  row[1],
                (String)  row[2],
                (String)  row[3],
                toInstant(row[4]),
                toInstant(row[5]),
                ((Number) row[6]).longValue(),
                (String)  row[7],
                (String)  row[9],
                (String)  row[10],
                row[8] != null ? ((Number) row[8]).longValue() : null,
                (String)  row[11],
                (String)  row[12],
                QuestionStatus.valueOf((String) row[13])
        );
    }

    private static Instant toInstant(Object val) {
        if (val == null)                    return null;
        if (val instanceof Instant inst)    return inst;
        if (val instanceof Timestamp ts)    return ts.toInstant();
        if (val instanceof OffsetDateTime o) return o.toInstant();
        throw new IllegalStateException("Unexpected timestamp type: " + val.getClass());
    }
}
