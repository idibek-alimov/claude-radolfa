package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.domain.model.CartStatus;
import tj.radolfa.infrastructure.persistence.entity.CartEntity;

import java.util.Optional;

public interface CartRepository extends JpaRepository<CartEntity, Long> {

    Optional<CartEntity> findByUserIdAndStatus(Long userId, CartStatus status);
}
