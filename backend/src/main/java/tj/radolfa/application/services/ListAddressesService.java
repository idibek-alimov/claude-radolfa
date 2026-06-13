package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.address.ListAddressesUseCase;
import tj.radolfa.application.ports.out.LoadAddressPort;
import tj.radolfa.domain.model.Address;

import java.util.List;

@Service
public class ListAddressesService implements ListAddressesUseCase {

    private final LoadAddressPort loadAddressPort;

    public ListAddressesService(LoadAddressPort loadAddressPort) {
        this.loadAddressPort = loadAddressPort;
    }

    @Override
    @Transactional
    public List<Address> execute(Long userId) {
        return loadAddressPort.findByUserId(userId);
    }
}
