package tj.radolfa.domain.model;

/**
 * Authorised roles.
 *
 * SYNC is the only role permitted to write authoritative-source-locked fields.
 */
public enum UserRole {
    USER,
    MANAGER,
    SYNC
}
