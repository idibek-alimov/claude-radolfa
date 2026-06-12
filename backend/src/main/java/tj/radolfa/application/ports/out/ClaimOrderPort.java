package tj.radolfa.application.ports.out;

import java.time.Instant;

public interface ClaimOrderPort {

    /**
     * Atomically claims an order for a courier via a conditional UPDATE.
     * The update only fires when {@code status = 'PICKED'}, {@code courier_id IS NULL},
     * and {@code delivery_type = 'HOME'}.
     *
     * @return {@code true} iff exactly one row was updated (the claim succeeded),
     *         {@code false} if the order was already claimed or not in a claimable state.
     */
    boolean claimIfAvailable(Long orderId, Long courierId, Instant claimedAt);
}
