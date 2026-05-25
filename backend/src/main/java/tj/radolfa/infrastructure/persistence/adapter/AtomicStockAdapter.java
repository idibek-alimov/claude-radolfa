package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.AtomicStockPort;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

@Component
public class AtomicStockAdapter implements AtomicStockPort {

    private final SkuRepository skuRepo;

    public AtomicStockAdapter(SkuRepository skuRepo) {
        this.skuRepo = skuRepo;
    }

    @Override
    public int decrementIfAvailable(Long skuId, int qty) {
        return skuRepo.decrementStockIfAvailable(skuId, qty);
    }

    @Override
    public int increment(Long skuId, int qty) {
        return skuRepo.incrementStock(skuId, qty);
    }
}
