package tj.radolfa.application.ports.out;

public interface ActivateProductPort {
    /** Idempotent: returns 1 the first time, 0 thereafter. */
    int activateIfAwaitingStock(Long productBaseId);
}
