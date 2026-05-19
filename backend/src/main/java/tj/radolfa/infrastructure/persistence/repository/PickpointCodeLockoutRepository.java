package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.PickpointCodeLockoutEntity;

import java.util.Optional;

public interface PickpointCodeLockoutRepository extends JpaRepository<PickpointCodeLockoutEntity, Long> {
    Optional<PickpointCodeLockoutEntity> findByPickpointId(Long pickpointId);
}
