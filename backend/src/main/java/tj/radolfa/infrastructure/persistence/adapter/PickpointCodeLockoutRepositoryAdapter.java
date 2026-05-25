package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadPickpointCodeLockoutPort;
import tj.radolfa.application.ports.out.SavePickpointCodeLockoutPort;
import tj.radolfa.domain.model.PickpointCodeLockout;
import tj.radolfa.infrastructure.persistence.entity.PickpointCodeLockoutEntity;
import tj.radolfa.infrastructure.persistence.repository.PickpointCodeLockoutRepository;

import java.util.Optional;

@Component
public class PickpointCodeLockoutRepositoryAdapter
        implements LoadPickpointCodeLockoutPort, SavePickpointCodeLockoutPort {

    private final PickpointCodeLockoutRepository repository;

    public PickpointCodeLockoutRepositoryAdapter(PickpointCodeLockoutRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PickpointCodeLockout> findByPickpointId(Long pickpointId) {
        return repository.findByPickpointId(pickpointId).map(this::toDomain);
    }

    @Override
    public PickpointCodeLockout save(PickpointCodeLockout lockout) {
        PickpointCodeLockoutEntity entity;
        if (lockout.getId() != null) {
            entity = repository.findById(lockout.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "PickpointCodeLockout not found: " + lockout.getId()));
            entity.setLockedUntil(lockout.getLockedUntil());
            entity.setFailedCount(lockout.getFailedCount());
        } else {
            entity = new PickpointCodeLockoutEntity(
                    null,
                    lockout.getPickpointId(),
                    lockout.getLockedUntil(),
                    lockout.getFailedCount());
        }
        PickpointCodeLockoutEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private PickpointCodeLockout toDomain(PickpointCodeLockoutEntity e) {
        return new PickpointCodeLockout(e.getId(), e.getPickpointId(), e.getLockedUntil(), e.getFailedCount());
    }
}
