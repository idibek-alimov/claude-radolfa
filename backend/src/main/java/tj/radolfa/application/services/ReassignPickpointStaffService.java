package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.user.ReassignPickpointStaffUseCase;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

@Service
public class ReassignPickpointStaffService implements ReassignPickpointStaffUseCase {

    private final LoadUserPort      loadUserPort;
    private final SaveUserPort      saveUserPort;
    private final LoadPickpointPort loadPickpointPort;

    public ReassignPickpointStaffService(LoadUserPort loadUserPort,
                                         SaveUserPort saveUserPort,
                                         LoadPickpointPort loadPickpointPort) {
        this.loadUserPort      = loadUserPort;
        this.saveUserPort      = saveUserPort;
        this.loadPickpointPort = loadPickpointPort;
    }

    @Override
    @Transactional
    public void execute(Long staffUserId, Long newPickpointId) {
        User user = loadUserPort.loadById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + staffUserId));

        if (user.role() != UserRole.PICKPOINT_STAFF) {
            throw new IllegalArgumentException(
                    "User " + staffUserId + " is not PICKPOINT_STAFF");
        }

        loadPickpointPort.findById(newPickpointId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pickpoint not found: " + newPickpointId));

        User updated = new User(
                user.id(),
                user.phone(),
                user.role(),
                user.name(),
                user.email(),
                user.loyalty(),
                user.enabled(),
                user.version(),
                user.vehicleType(),
                user.maxPayloadKg(),
                user.maxLengthCm(),
                user.maxWidthCm(),
                user.maxHeightCm(),
                newPickpointId,
                user.deliveryZoneId());

        saveUserPort.save(updated);
    }
}
