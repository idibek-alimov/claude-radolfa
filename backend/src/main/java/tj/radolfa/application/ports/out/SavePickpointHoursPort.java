package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PickpointHours;

import java.util.List;

public interface SavePickpointHoursPort {
    List<PickpointHours> replaceAll(Long pickpointId, List<PickpointHours> hours);
}
