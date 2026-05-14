package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UpdatePickpointHoursUseCase;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.SavePickpointHoursPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.PickpointHours;

import java.util.List;

@Service
@Transactional
public class UpdatePickpointHoursService implements UpdatePickpointHoursUseCase {

    private final LoadPickpointPort loadPickpointPort;
    private final SavePickpointHoursPort savePickpointHoursPort;

    public UpdatePickpointHoursService(LoadPickpointPort loadPickpointPort,
                                       SavePickpointHoursPort savePickpointHoursPort) {
        this.loadPickpointPort = loadPickpointPort;
        this.savePickpointHoursPort = savePickpointHoursPort;
    }

    @Override
    public List<PickpointHours> execute(Long pickpointId, List<PickpointHours> hours) {
        loadPickpointPort.findById(pickpointId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pickpoint not found: " + pickpointId));
        return savePickpointHoursPort.replaceAll(pickpointId, hours);
    }
}
