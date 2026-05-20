package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.PageResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoadOrderPort {
    List<Order> loadByUserId(Long userId);

    default PageResult<Order> loadByUserIdPaged(Long userId, int page, int size) {
        return new PageResult<>(List.of(), 0, page, size, true);
    }

    Optional<Order> loadById(Long id);

    Optional<Order> loadByExternalOrderId(String externalOrderId);

    /** Returns the most recent {@code limit} PAID orders for the given user, newest first. */
    List<Order> loadRecentPaidByUserId(Long userId, int limit);

    /** Returns all PENDING orders created before {@code cutoff} — used by the payment sweep job. */
    default List<Order> findExpiredPending(Instant cutoff) { return List.of(); }
}
