package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.GetPickpointHoursUseCase;
import tj.radolfa.application.ports.out.LoadPickpointHoursPort;
import tj.radolfa.domain.model.PickpointHours;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetPickpointHoursService implements GetPickpointHoursUseCase {

    private final LoadPickpointHoursPort port;

    public GetPickpointHoursService(LoadPickpointHoursPort port) {
        this.port = port;
    }

    @Override
    public List<PickpointHours> execute(Long pickpointId) {
        return port.findByPickpointId(pickpointId);
    }
}
