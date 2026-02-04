package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tj.radolfa.infrastructure.persistence.entity.UserEntity;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserEntity}.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPhone(String phone);
}
