package tj.radolfa.application.ports.in;

public interface UpdateLoyaltyTierUseCase {
    record UpdateTierColorCommand(Long tierId, String color) {}
    void updateColor(UpdateTierColorCommand command);
}
