package tj.radolfa.application.ports.in.address;

public interface DeleteAddressUseCase {

    void execute(Command command);

    record Command(Long id, Long userId) {}
}
