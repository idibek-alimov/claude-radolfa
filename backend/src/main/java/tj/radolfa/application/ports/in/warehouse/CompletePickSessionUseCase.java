package tj.radolfa.application.ports.in.warehouse;

/**
 * In-Port: complete a pick session, transitioning the order from PAID to PICKED.
 *
 * <p>Requires that every item in the order is fully picked. Call after all units
 * have been scanned via {@link ScanOrderItemUnitUseCase} and the scan result
 * reports {@code orderFullyPicked = true}.
 */
public interface CompletePickSessionUseCase {
    record Command(Long orderId, Long actorUserId) {}
    void execute(Command cmd);
}
