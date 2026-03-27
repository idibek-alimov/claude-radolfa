package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tj.radolfa.infrastructure.persistence.entity.UserEntity;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserEntity}.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.tier LEFT JOIN FETCH u.lowestTierEver WHERE u.phone = :phone")
    Optional<UserEntity> findByPhone(@Param("phone") String phone);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.tier LEFT JOIN FETCH u.lowestTierEver WHERE u.id = :id")
    Optional<UserEntity> findByIdWithTier(@Param("id") Long id);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.tier LEFT JOIN FETCH u.lowestTierEver WHERE u.loyaltyPermanent = false")
    List<UserEntity> findAllNonPermanent();

    @Query(value = "SELECT u FROM UserEntity u LEFT JOIN FETCH u.tier WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))",
            countQuery = "SELECT COUNT(u) FROM UserEntity u WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<UserEntity> searchUsers(@Param("query") String query, Pageable pageable);
}
