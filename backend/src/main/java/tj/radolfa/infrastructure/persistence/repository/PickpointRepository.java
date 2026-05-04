package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.PickpointEntity;

import java.util.List;

public interface PickpointRepository extends JpaRepository<PickpointEntity, Long> {
    List<PickpointEntity> findAllByActiveTrue();
}
