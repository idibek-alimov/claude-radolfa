package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ProductBase;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface LoadProductBasePort {

    Optional<ProductBase> findByExternalRef(String externalRef);

    Optional<ProductBase> findById(Long id);

    Map<Long, ProductBase> findProductsByIds(Collection<Long> ids);
}
