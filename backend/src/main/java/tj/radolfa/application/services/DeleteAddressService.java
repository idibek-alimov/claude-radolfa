package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.address.DeleteAddressUseCase;
import tj.radolfa.application.ports.out.LoadAddressPort;
import tj.radolfa.application.ports.out.SaveAddressPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;

@Service
public class DeleteAddressService implements DeleteAddressUseCase {

    private final LoadAddressPort loadAddressPort;
    private final SaveAddressPort saveAddressPort;

    public DeleteAddressService(LoadAddressPort loadAddressPort, SaveAddressPort saveAddressPort) {
        this.loadAddressPort = loadAddressPort;
        this.saveAddressPort = saveAddressPort;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        loadAddressPort.findByIdAndUserId(command.id(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found: id=" + command.id()));

        saveAddressPort.delete(command.id(), command.userId());
    }
}
