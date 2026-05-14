package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ProductTag;

import java.util.List;
import java.util.Optional;

public interface LoadProductTagPort {
    List<ProductTag> findAll();
    Optional<ProductTag> findById(Long id);
    List<ProductTag> findAllByIds(List<Long> ids);
    boolean existsByName(String name);
    boolean existsByNameExcludingId(String name, Long excludeId);
    long countVariantsUsingTag(Long tagId);
}
