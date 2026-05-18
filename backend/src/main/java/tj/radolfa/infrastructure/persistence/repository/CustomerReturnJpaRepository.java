package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.infrastructure.persistence.entity.CustomerReturnEntity;

import java.util.List;

public interface CustomerReturnJpaRepository extends JpaRepository<CustomerReturnEntity, Long> {

    List<CustomerReturnEntity> findAllByOrderId(Long orderId);

    Page<CustomerReturnEntity> findByPickpointIdAndStatus(Long pickpointId, CustomerReturnStatus status, Pageable pageable);

    @Query(
        value = """
            SELECT cr.* FROM customer_returns cr
            LEFT JOIN orders o ON cr.order_id = o.id
            LEFT JOIN users  u ON o.user_id   = u.id
            WHERE :search IS NULL OR :search = ''
               OR CAST(cr.order_id AS TEXT) LIKE CONCAT('%', :search, '%')
               OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
            """,
        countQuery = """
            SELECT COUNT(cr.id) FROM customer_returns cr
            LEFT JOIN orders o ON cr.order_id = o.id
            LEFT JOIN users  u ON o.user_id   = u.id
            WHERE :search IS NULL OR :search = ''
               OR CAST(cr.order_id AS TEXT) LIKE CONCAT('%', :search, '%')
               OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
            """,
        nativeQuery = true
    )
    Page<CustomerReturnEntity> findAllWithSearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT cr.pickpointId, COUNT(cr) FROM CustomerReturnEntity cr WHERE cr.status = :status GROUP BY cr.pickpointId")
    List<Object[]> countByStatusGroupByPickpoint(@Param("status") CustomerReturnStatus status);
}
