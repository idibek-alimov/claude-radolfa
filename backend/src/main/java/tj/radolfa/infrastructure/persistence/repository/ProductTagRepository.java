package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.ProductTagEntity;

import java.util.Optional;

public interface ProductTagRepository extends JpaRepository<ProductTagEntity, Long> {
    Optional<ProductTagEntity> findByName(String name);
    boolean existsByName(String name);
}
