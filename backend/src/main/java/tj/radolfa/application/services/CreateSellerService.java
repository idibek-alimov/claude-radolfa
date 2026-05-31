package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.seller.CreateSellerUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveSellerPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.Seller;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

@Service
public class CreateSellerService implements CreateSellerUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final SaveSellerPort saveSellerPort;

    public CreateSellerService(LoadUserPort loadUserPort,
                               SaveUserPort saveUserPort,
                               SaveSellerPort saveSellerPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.saveSellerPort = saveSellerPort;
    }

    @Override
    @Transactional
    public Seller execute(Command command) {
        if (loadUserPort.loadByPhone(command.phone()).isPresent()) {
            throw new DuplicateResourceException(
                    "User with phone '" + command.phone() + "' already exists");
        }

        User user = new User(
                null,
                new PhoneNumber(command.phone()),
                UserRole.SELLER,
                null,
                null,
                LoyaltyProfile.empty(),
                true,
                null);

        User savedUser = saveUserPort.save(user);

        Seller seller = new Seller(
                null,
                savedUser.id(),
                command.shopName(),
                command.logoUrl(),
                command.bio(),
                null);

        return saveSellerPort.save(seller);
    }
}
