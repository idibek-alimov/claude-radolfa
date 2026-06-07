package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.FeaturedCategory;
import tj.radolfa.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface LoadFeaturedCategoryPort {
    Optional<FeaturedCategory> findById(Long id);

    /** Active entries ordered by displayOrder ascending — used by the public storefront read. */
    List<FeaturedCategory> findActiveOrdered();

    /** Server-side paginated admin list. {@code page} is 1-based; {@code sort} is whitelisted by the adapter. */
    PageResult<FeaturedCategory> findAllPaged(int page, int size, String search, String sort);
}
