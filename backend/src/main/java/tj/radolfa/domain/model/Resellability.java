package tj.radolfa.domain.model;

public enum Resellability {
    PENDING_REVIEW,  // default — not yet assessed
    RESELLABLE,      // item is in good condition; stock will be restored
    DEFECTIVE        // item is damaged or unsellable; stock written off
}
