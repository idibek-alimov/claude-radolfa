package tj.radolfa.domain.exception;

import java.time.Instant;

public class PickpointCodeLockoutException extends RuntimeException {

    private final Instant lockedUntil;

    public PickpointCodeLockoutException(Instant lockedUntil) {
        super("Too many failed attempts at this pickup point. Locked until " + lockedUntil);
        this.lockedUntil = lockedUntil;
    }

    public Instant getLockedUntil() { return lockedUntil; }
}
