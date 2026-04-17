package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.Discount;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadDiscountPort {

    Optional<Discount> findById(Long id);

    List<Discount> findActiveByItemCode(String itemCode);

    List<Discount> findActiveByItemCodes(Collection<String> itemCodes);

    Page<Discount> findAll(DiscountFilter filter, Pageable pageable);
}
