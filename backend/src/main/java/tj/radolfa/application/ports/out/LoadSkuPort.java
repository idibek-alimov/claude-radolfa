package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Sku;

import java.util.List;
import java.util.Optional;

public interface LoadSkuPort {

    Optional<Sku> findBySkuCode(String skuCode);

    Optional<Sku> findSkuById(Long id);

    List<Sku> findSkusByVariantId(Long variantId);
}
