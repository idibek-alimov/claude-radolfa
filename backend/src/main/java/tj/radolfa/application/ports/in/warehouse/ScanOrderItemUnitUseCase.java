package tj.radolfa.application.ports.in.warehouse;

public interface ScanOrderItemUnitUseCase {

    record Command(Long orderId, String scannedBarcode, Long actorUserId) {}

    record Result(Long orderItemId, int quantityPicked, int quantityOrdered,
                  boolean orderFullyPicked) {}

    Result execute(Command cmd);
}
