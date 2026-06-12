package tj.radolfa.application.ports.in.product;

public interface SubmitProductForReviewUseCase {
    void execute(Long productBaseId, ProductActor actor);
}
