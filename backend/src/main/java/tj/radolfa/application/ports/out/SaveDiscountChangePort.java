package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.DiscountChange;

public interface SaveDiscountChangePort {
    DiscountChange save(DiscountChange change);
}
