package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Pickpoint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadPickpointPort {
    List<Pickpoint> findAll(String search);
    List<Pickpoint> findAllActive();
    Optional<Pickpoint> findById(Long id);
    default List<Pickpoint> findAllByIds(Collection<Long> ids) { return List.of(); }
}
