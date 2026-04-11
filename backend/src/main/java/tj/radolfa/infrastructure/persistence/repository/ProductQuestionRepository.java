package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.domain.model.QuestionStatus;
import tj.radolfa.infrastructure.persistence.entity.ProductQuestionEntity;

import java.util.List;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestionEntity, Long> {

    Page<ProductQuestionEntity> findByProductBaseIdAndStatus(Long productBaseId, QuestionStatus status, Pageable pageable);

    Page<ProductQuestionEntity> findByStatusOrderByCreatedAtAsc(QuestionStatus status, Pageable pageable);

    @Query(nativeQuery = true, value = """
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
                COALESCE(c_asked.hex_code,     c_first.hex_code)     AS color_hex
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
            WHERE pq.status = 'PENDING'
            ORDER BY pq.created_at ASC
            LIMIT :limit
            """)
    List<Object[]> findPendingWithContext(@Param("limit") int limit);
}
