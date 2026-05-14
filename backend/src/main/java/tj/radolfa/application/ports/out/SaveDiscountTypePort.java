package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.DiscountType;

public interface SaveDiscountTypePort {

    DiscountType save(DiscountType type);

    void deleteById(Long id);
}
