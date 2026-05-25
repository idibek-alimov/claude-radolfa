package tj.radolfa.application.ports.in.order;

public interface VerifyPickupByCodeUseCase {
    void execute(String code, Long staffUserId);
}
