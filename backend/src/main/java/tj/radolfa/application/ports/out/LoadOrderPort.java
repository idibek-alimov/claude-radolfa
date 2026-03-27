package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Order;
import java.util.List;
import java.util.Optional;

public interface LoadOrderPort {
    List<Order> loadByUserId(Long userId);

    Optional<Order> loadById(Long id);

    Optional<Order> loadByExternalOrderId(String externalOrderId);

    /** Returns the most recent {@code limit} PAID orders for the given user, newest first. */
    List<Order> loadRecentPaidByUserId(Long userId, int limit);
}
