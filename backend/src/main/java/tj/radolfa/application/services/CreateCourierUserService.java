package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.user.CreateCourierUserUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

@Service
public class CreateCourierUserService implements CreateCourierUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    public CreateCourierUserService(LoadUserPort loadUserPort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    @Transactional
    public Long execute(Command command) {
        if (loadUserPort.loadByPhone(command.phone()).isPresent()) {
            throw new DuplicateResourceException(
                    "User with phone '" + command.phone() + "' already exists");
        }

        User user = new User(
                null,
                new PhoneNumber(command.phone()),
                UserRole.COURIER,
                command.name(),
                null,
                LoyaltyProfile.empty(),
                true,
                null,
                command.vehicleType(),
                command.maxPayloadKg(),
                command.maxLengthCm(),
                command.maxWidthCm(),
                command.maxHeightCm(),
                null,
                null);

        return saveUserPort.save(user).id();
    }
}
