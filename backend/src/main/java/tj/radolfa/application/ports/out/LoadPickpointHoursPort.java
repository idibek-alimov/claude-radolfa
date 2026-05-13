package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PickpointHours;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface LoadPickpointHoursPort {
    List<PickpointHours> findByPickpointId(Long pickpointId);
    Map<Long, List<PickpointHours>> findByPickpointIds(Collection<Long> ids);
}
