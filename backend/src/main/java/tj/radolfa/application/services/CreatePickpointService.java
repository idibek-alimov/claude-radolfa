package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.CreatePickpointUseCase;
import tj.radolfa.application.ports.out.SavePickpointPort;
import tj.radolfa.domain.model.Pickpoint;

@Service
@Transactional
public class CreatePickpointService implements CreatePickpointUseCase {

    private final SavePickpointPort savePickpointPort;

    public CreatePickpointService(SavePickpointPort savePickpointPort) {
        this.savePickpointPort = savePickpointPort;
    }

    @Override
    public Pickpoint execute(String name, String address) {
        return savePickpointPort.save(name, address);
    }
}
