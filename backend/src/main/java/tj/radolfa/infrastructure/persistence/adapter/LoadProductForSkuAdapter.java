package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadProductForSkuPort;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

import java.util.Optional;

@Component
public class LoadProductForSkuAdapter implements LoadProductForSkuPort {

    private final SkuRepository skuRepository;

    public LoadProductForSkuAdapter(SkuRepository skuRepository) {
        this.skuRepository = skuRepository;
    }

    @Override
    public Optional<Long> findProductBaseIdBySkuId(Long skuId) {
        return skuRepository.findProductBaseIdBySkuId(skuId);
    }
}
