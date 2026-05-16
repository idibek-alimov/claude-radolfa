package tj.radolfa.application.ports.in.user;

public interface CreatePickpointStaffUseCase {
    record Command(String phone, String name, Long pickpointId) {}
    Long execute(Command command);
}
