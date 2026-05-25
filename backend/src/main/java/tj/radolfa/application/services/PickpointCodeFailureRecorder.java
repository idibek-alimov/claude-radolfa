package tj.radolfa.application.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.out.LoadPickpointCodeLockoutPort;
import tj.radolfa.application.ports.out.SavePickpointCodeLockoutPort;
import tj.radolfa.domain.model.PickpointCodeLockout;

/**
 * Records a failed pickup-code verification attempt against the pickpoint's lockout counter.
 *
 * <p>Runs in its own {@code REQUIRES_NEW} transaction so the counter write commits even
 * when the outer {@link VerifyPickupByCodeService} transaction rolls back due to the
 * thrown verification exception.
 */
@Service
public class PickpointCodeFailureRecorder {

    private final LoadPickpointCodeLockoutPort loadPort;
    private final SavePickpointCodeLockoutPort savePort;
    private final int                          lockoutThreshold;
    private final int                          lockoutMinutes;

    public PickpointCodeFailureRecorder(
            LoadPickpointCodeLockoutPort loadPort,
            SavePickpointCodeLockoutPort savePort,
            @Value("${radolfa.delivery.code-lockout-threshold:20}") int lockoutThreshold,
            @Value("${radolfa.delivery.code-lockout-minutes:30}")   int lockoutMinutes) {
        this.loadPort         = loadPort;
        this.savePort         = savePort;
        this.lockoutThreshold = lockoutThreshold;
        this.lockoutMinutes   = lockoutMinutes;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long pickpointId) {
        PickpointCodeLockout lockout = loadPort.findByPickpointId(pickpointId)
                .orElseGet(() -> PickpointCodeLockout.newForPickpoint(pickpointId));
        lockout.recordFailure(lockoutThreshold, lockoutMinutes);
        savePort.save(lockout);
    }
}
