package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;

import java.util.Optional;

public interface ProductBaseRepository extends JpaRepository<ProductBaseEntity, Long> {

    Optional<ProductBaseEntity> findByErpTemplateCode(String erpTemplateCode);
}
