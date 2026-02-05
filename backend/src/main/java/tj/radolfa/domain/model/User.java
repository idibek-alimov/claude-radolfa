package tj.radolfa.domain.model;

/**
 * Immutable domain representation of an application user.
 * Now includes profile information (name, email).
 */
public record User(
        Long id,
        String phone,
        UserRole role,
        String name,
        String email) {
}
