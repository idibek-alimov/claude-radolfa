package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.command.UpdatePickpointCommand;
import tj.radolfa.application.ports.in.UpdatePickpointUseCase;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.SavePickpointPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Pickpoint;

@Service
@Transactional
public class UpdatePickpointService implements UpdatePickpointUseCase {

    private final LoadPickpointPort loadPickpointPort;
    private final SavePickpointPort savePickpointPort;

    public UpdatePickpointService(LoadPickpointPort loadPickpointPort,
                                  SavePickpointPort savePickpointPort) {
        this.loadPickpointPort = loadPickpointPort;
        this.savePickpointPort = savePickpointPort;
    }

    @Override
    public Pickpoint execute(UpdatePickpointCommand command) {
        loadPickpointPort.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pickpoint not found: " + command.id()));
        return savePickpointPort.update(command);
    }
}
