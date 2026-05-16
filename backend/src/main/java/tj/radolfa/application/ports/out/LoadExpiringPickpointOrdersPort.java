package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Order;

import java.time.Instant;
import java.util.List;

public interface LoadExpiringPickpointOrdersPort {

    /** READY_FOR_PICKUP orders with readyForPickupAt strictly before the cutoff. */
    List<Order> findReadyForPickupOlderThan(Instant cutoff);

    /** READY_FOR_PICKUP orders with readyForPickupAt in [startInclusive, endExclusive). */
    List<Order> findReadyForPickupInWindow(Instant startInclusive, Instant endExclusive);
}
