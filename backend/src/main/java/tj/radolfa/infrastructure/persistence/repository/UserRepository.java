package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tj.radolfa.infrastructure.persistence.entity.UserEntity;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserEntity}.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPhone(String phone);

    @Query("SELECT u FROM UserEntity u WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<UserEntity> searchUsers(@Param("query") String query, Pageable pageable);
}
