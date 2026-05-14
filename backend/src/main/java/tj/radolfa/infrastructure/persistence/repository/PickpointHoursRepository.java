package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.PickpointHoursEntity;

import java.util.Collection;
import java.util.List;

public interface PickpointHoursRepository extends JpaRepository<PickpointHoursEntity, Long> {
    List<PickpointHoursEntity> findAllByPickpointIdOrderByDayOfWeek(Long pickpointId);

    List<PickpointHoursEntity> findAllByPickpointIdInOrderByPickpointIdAscDayOfWeekAsc(
            Collection<Long> pickpointIds);

    void deleteAllByPickpointId(Long pickpointId);
}
