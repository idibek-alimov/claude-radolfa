package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.StockReceiptEntity;

public interface StockReceiptRepository extends JpaRepository<StockReceiptEntity, Long> {

    @Query(
        value = """
            SELECT sr.* FROM stock_receipts sr
            WHERE :search IS NULL OR :search = ''
               OR LOWER(sr.supplier_reference) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY sr.created_at DESC
            """,
        countQuery = """
            SELECT COUNT(sr.id) FROM stock_receipts sr
            WHERE :search IS NULL OR :search = ''
               OR LOWER(sr.supplier_reference) LIKE LOWER(CONCAT('%', :search, '%'))
            """,
        nativeQuery = true
    )
    Page<StockReceiptEntity> findAllWithSearch(@Param("search") String search, Pageable pageable);
}
