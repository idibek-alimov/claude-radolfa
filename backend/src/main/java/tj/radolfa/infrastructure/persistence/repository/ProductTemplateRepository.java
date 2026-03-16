package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.ProductTemplateEntity;

import java.util.Optional;

public interface ProductTemplateRepository extends JpaRepository<ProductTemplateEntity, Long> {

    Optional<ProductTemplateEntity> findByErpTemplateCode(String erpTemplateCode);
}
