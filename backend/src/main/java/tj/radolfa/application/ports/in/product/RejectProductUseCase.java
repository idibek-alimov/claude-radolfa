package tj.radolfa.application.ports.in.product;

public interface RejectProductUseCase {
    record Command(Long productBaseId, String rejectionReason, Long actorUserId) {}
    void execute(Command cmd);
}
