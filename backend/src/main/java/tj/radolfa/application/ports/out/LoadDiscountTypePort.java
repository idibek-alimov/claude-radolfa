package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.DiscountType;

import java.util.List;
import java.util.Optional;

public interface LoadDiscountTypePort {

    List<DiscountType> findAll();

    Optional<DiscountType> findById(Long id);

    long countDiscountsByTypeId(Long typeId);
}
