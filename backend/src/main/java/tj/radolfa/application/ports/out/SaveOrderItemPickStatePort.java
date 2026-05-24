package tj.radolfa.application.ports.out;

public interface SaveOrderItemPickStatePort {

    /**
     * Atomically increments quantity_picked by 1 and stamps picked_at /
     * picked_by_user_id on the last scan (when after-increment equals quantity).
     * Returns the updated picked quantity.
     * Throws OrderItemAlreadyFullyPickedException if already at quantity.
     */
    int incrementPicked(Long orderItemId, Long actorUserId);
}
