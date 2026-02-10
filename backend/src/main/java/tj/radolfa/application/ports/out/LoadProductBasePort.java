package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ProductBase;

import java.util.Optional;

public interface LoadProductBasePort {

    Optional<ProductBase> findByErpTemplateCode(String erpTemplateCode);
}
