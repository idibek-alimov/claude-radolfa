package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.user.UpdateCourierDetailsUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

@Service
public class UpdateCourierDetailsService implements UpdateCourierDetailsUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    public UpdateCourierDetailsService(LoadUserPort loadUserPort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        User user = loadUserPort.loadById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + command.userId()));

        if (user.role() != UserRole.COURIER) {
            throw new IllegalArgumentException(
                    "User " + command.userId() + " is not a COURIER");
        }

        User updated = new User(
                user.id(),
                user.phone(),
                user.role(),
                user.name(),
                user.email(),
                user.loyalty(),
                user.enabled(),
                user.version(),
                command.vehicleType(),
                command.maxPayloadKg(),
                command.maxLengthCm(),
                command.maxWidthCm(),
                command.maxHeightCm(),
                user.pickpointId(),
                user.deliveryZoneId());

        saveUserPort.save(updated);
    }
}
