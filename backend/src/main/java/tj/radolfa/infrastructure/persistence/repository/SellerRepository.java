package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.SellerEntity;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link SellerEntity}.
 */
public interface SellerRepository extends JpaRepository<SellerEntity, Long> {

    Optional<SellerEntity> findByUserId(Long userId);

    @Query(value = "SELECT s FROM SellerEntity s WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :query, '%')))",
            countQuery = "SELECT COUNT(s) FROM SellerEntity s WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<SellerEntity> searchSellers(@Param("query") String query, Pageable pageable);
}
