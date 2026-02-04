package tj.radolfa.domain.model;

/**
 * Immutable domain representation of an application user.
 * Record is appropriate here – no mutable merge logic required.
 */
public record User(Long id, String phone, UserRole role) {}
