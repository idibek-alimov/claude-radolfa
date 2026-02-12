package tj.radolfa.domain.model;

/**
 * Immutable domain representation of an application user.
 * Now includes profile information (name, email).
 */
public record User(
                Long id,
                PhoneNumber phone,
                UserRole role,
                String name,
                String email,
                int loyaltyPoints,
                Long version) {
}
