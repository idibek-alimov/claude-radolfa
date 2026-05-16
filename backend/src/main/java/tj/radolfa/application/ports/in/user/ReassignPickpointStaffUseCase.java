package tj.radolfa.application.ports.in.user;

public interface ReassignPickpointStaffUseCase {
    void execute(Long staffUserId, Long newPickpointId);
}
