package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.user.CreatePickpointStaffUseCase;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

@Service
public class CreatePickpointStaffService implements CreatePickpointStaffUseCase {

    private final LoadUserPort     loadUserPort;
    private final SaveUserPort     saveUserPort;
    private final LoadPickpointPort loadPickpointPort;

    public CreatePickpointStaffService(LoadUserPort loadUserPort,
                                       SaveUserPort saveUserPort,
                                       LoadPickpointPort loadPickpointPort) {
        this.loadUserPort     = loadUserPort;
        this.saveUserPort     = saveUserPort;
        this.loadPickpointPort = loadPickpointPort;
    }

    @Override
    @Transactional
    public Long execute(Command command) {
        if (loadUserPort.loadByPhone(command.phone()).isPresent()) {
            throw new DuplicateResourceException(
                    "User with phone '" + command.phone() + "' already exists");
        }

        loadPickpointPort.findById(command.pickpointId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pickpoint not found: " + command.pickpointId()));

        User user = new User(
                null,
                new PhoneNumber(command.phone()),
                UserRole.PICKPOINT_STAFF,
                command.name(),
                null,
                LoyaltyProfile.empty(),
                true,
                null,
                null, null, null, null, null,
                command.pickpointId(),
                null);

        return saveUserPort.save(user).id();
    }
}
