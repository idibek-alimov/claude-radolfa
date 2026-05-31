package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Seller;

import java.util.Optional;

public interface LoadSellerPort {
    Optional<Seller> findByUserId(Long userId);
    Optional<Seller> findById(Long id);
    PageResult<Seller> findAllPaged(int page, int size, String search);
}
