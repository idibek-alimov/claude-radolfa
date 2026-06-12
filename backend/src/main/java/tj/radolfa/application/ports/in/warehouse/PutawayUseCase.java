package tj.radolfa.application.ports.in.warehouse;

public interface PutawayUseCase {
    record Command(Long skuId, Long binId, int quantity, Long actorUserId) {}
    void execute(Command cmd);
}
