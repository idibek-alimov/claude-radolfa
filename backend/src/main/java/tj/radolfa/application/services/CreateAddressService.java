package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.address.CreateAddressUseCase;
import tj.radolfa.application.ports.out.LoadAddressPort;
import tj.radolfa.application.ports.out.SaveAddressPort;
import tj.radolfa.domain.model.Address;

@Service
public class CreateAddressService implements CreateAddressUseCase {

    private final LoadAddressPort loadAddressPort;
    private final SaveAddressPort saveAddressPort;

    public CreateAddressService(LoadAddressPort loadAddressPort, SaveAddressPort saveAddressPort) {
        this.loadAddressPort = loadAddressPort;
        this.saveAddressPort = saveAddressPort;
    }

    @Override
    @Transactional
    public Address execute(Command command) {
        // The very first address in the book is always the default, regardless of what was requested.
        boolean isFirstAddress = loadAddressPort.findByUserId(command.userId()).isEmpty();
        boolean shouldBeDefault = command.isDefault() || isFirstAddress;

        Address address = new Address(
                null,
                command.userId(),
                command.label(),
                command.recipientName(),
                command.phone(),
                command.line1(),
                command.city(),
                command.postalCode(),
                command.country(),
                shouldBeDefault
        );

        if (shouldBeDefault) {
            saveAddressPort.clearDefaultForUser(command.userId());
        }

        return saveAddressPort.save(address);
    }
}
