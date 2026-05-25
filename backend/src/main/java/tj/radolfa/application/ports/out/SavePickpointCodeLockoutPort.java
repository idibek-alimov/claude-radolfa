package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PickpointCodeLockout;

public interface SavePickpointCodeLockoutPort {
    PickpointCodeLockout save(PickpointCodeLockout lockout);
}
