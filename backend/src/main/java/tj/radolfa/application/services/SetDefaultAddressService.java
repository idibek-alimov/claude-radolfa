package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.address.SetDefaultAddressUseCase;
import tj.radolfa.application.ports.out.LoadAddressPort;
import tj.radolfa.application.ports.out.SaveAddressPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Address;

@Service
public class SetDefaultAddressService implements SetDefaultAddressUseCase {

    private final LoadAddressPort loadAddressPort;
    private final SaveAddressPort saveAddressPort;

    public SetDefaultAddressService(LoadAddressPort loadAddressPort, SaveAddressPort saveAddressPort) {
        this.loadAddressPort = loadAddressPort;
        this.saveAddressPort = saveAddressPort;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        Address address = loadAddressPort.findByIdAndUserId(command.id(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found: id=" + command.id()));

        // Clear-then-set, keyed by userId, in one transaction — the default flag stays exclusive.
        saveAddressPort.clearDefaultForUser(command.userId());
        address.markDefault();
        saveAddressPort.save(address);
    }
}
