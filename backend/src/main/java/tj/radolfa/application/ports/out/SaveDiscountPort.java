package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Discount;

public interface SaveDiscountPort {

    Discount save(Discount discount);
}
