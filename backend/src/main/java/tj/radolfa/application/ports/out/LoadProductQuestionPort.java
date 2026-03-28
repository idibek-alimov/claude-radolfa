package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.ProductQuestion;

import java.util.List;
import java.util.Optional;

public interface LoadProductQuestionPort {

    Optional<ProductQuestion> findById(Long id);

    Page<ProductQuestion> findPublishedByProductBase(Long productBaseId, Pageable pageable);

    /** Returns the oldest pending questions up to {@code limit} — used by the admin queue. */
    List<ProductQuestion> findPendingOldestFirst(int limit);
}
