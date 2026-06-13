package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.address.UpdateAddressUseCase;
import tj.radolfa.application.ports.out.LoadAddressPort;
import tj.radolfa.application.ports.out.SaveAddressPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Address;

@Service
public class UpdateAddressService implements UpdateAddressUseCase {

    private final LoadAddressPort loadAddressPort;
    private final SaveAddressPort saveAddressPort;

    public UpdateAddressService(LoadAddressPort loadAddressPort, SaveAddressPort saveAddressPort) {
        this.loadAddressPort = loadAddressPort;
        this.saveAddressPort = saveAddressPort;
    }

    @Override
    @Transactional
    public Address execute(Command command) {
        Address address = loadAddressPort.findByIdAndUserId(command.id(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found: id=" + command.id()));

        address.updateDetails(
                command.label(),
                command.recipientName(),
                command.phone(),
                command.line1(),
                command.city(),
                command.postalCode(),
                command.country()
        );

        return saveAddressPort.save(address);
    }
}
