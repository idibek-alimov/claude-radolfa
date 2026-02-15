package tj.radolfa.domain.model;

/**
 * Immutable domain representation of an application user.
 */
public record User(
                Long id,
                PhoneNumber phone,
                UserRole role,
                String name,
                String email,
                int loyaltyPoints,
                boolean enabled,
                Long version) {
}
