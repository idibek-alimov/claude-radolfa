package tj.radolfa.application.ports.in.order;

public interface ExpireOrderUseCase {
    /**
     * System-driven cancellation (no requester check). Used by scheduled jobs.
     * Restores stock and loyalty points exactly like a user-requested cancellation.
     */
    void execute(Long orderId, String reason);
}
