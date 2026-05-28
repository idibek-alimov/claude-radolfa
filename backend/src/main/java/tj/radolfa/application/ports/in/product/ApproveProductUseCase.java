package tj.radolfa.application.ports.in.product;

public interface ApproveProductUseCase {
    void execute(Long productBaseId, Long actorUserId);
}
