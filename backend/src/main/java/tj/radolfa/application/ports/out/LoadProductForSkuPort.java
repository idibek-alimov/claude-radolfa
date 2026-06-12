package tj.radolfa.application.ports.out;

import java.util.Optional;

public interface LoadProductForSkuPort {
    Optional<Long> findProductBaseIdBySkuId(Long skuId);
}
