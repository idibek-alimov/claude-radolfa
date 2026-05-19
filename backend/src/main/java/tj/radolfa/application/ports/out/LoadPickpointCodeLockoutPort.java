package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PickpointCodeLockout;

import java.util.Optional;

public interface LoadPickpointCodeLockoutPort {
    Optional<PickpointCodeLockout> findByPickpointId(Long pickpointId);
}
