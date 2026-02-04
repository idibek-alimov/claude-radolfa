package tj.radolfa.domain.model;

/**
 * Authorised roles.
 *
 * SYSTEM is the only role permitted to write ERP-locked fields.
 */
public enum UserRole {
    USER,
    MANAGER,
    SYSTEM
}
