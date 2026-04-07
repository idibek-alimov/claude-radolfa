package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Sku;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface LoadSkuPort {

    Optional<Sku> findBySkuCode(String skuCode);

    Optional<Sku> findSkuById(Long id);

    List<Sku> findSkusByVariantId(Long variantId);

    List<Sku> findAllByIds(Collection<Long> ids);

    default Map<Long, Sku> findAllByIdsAsMap(Collection<Long> ids) {
        return findAllByIds(ids).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
    }
}
