package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.ListActivePickpointsUseCase;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.domain.model.Pickpoint;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListActivePickpointsService implements ListActivePickpointsUseCase {

    private final LoadPickpointPort loadPickpointPort;

    public ListActivePickpointsService(LoadPickpointPort loadPickpointPort) {
        this.loadPickpointPort = loadPickpointPort;
    }

    @Override
    public List<Pickpoint> execute() {
        return loadPickpointPort.findAllActive();
    }
}
