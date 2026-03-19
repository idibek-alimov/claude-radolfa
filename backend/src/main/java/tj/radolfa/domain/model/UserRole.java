package tj.radolfa.domain.model;

/**
 * Authorised roles.
 *
 * SYNC is the only role permitted to write authoritative-source-locked fields.
 */
public enum UserRole {
    USER,
    MANAGER,
    ADMIN,   // full platform administration — price, stock, orders, user management
    SYNC     // machine-to-machine import role; removed in Phase 10
}
