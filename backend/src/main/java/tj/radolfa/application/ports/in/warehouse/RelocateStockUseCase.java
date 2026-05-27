package tj.radolfa.application.ports.in.warehouse;

public interface RelocateStockUseCase {
    record Command(Long skuId, Long fromBinId, Long toBinId, int quantity, Long actorUserId) {}
    void execute(Command cmd);
}
