package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ProductTemplate;

import java.util.Optional;

public interface LoadProductTemplatePort {

    Optional<ProductTemplate> findByErpTemplateCode(String erpTemplateCode);

    Optional<ProductTemplate> findById(Long id);
}
