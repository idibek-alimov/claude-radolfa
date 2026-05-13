package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.PickpointHours;

import java.util.List;

public interface GetPickpointHoursUseCase {
    List<PickpointHours> execute(Long pickpointId);
}
