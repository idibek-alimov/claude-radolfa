package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.SkuPriceChange;

public interface SavePriceChangePort {
    SkuPriceChange save(SkuPriceChange change);
}
