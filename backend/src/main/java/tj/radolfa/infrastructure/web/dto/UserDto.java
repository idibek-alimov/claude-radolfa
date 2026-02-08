package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.User;

/**
 * DTO representing user information in API responses.
 */
public record UserDto(
        Long id,
        String phone,
        String role
) {
    /**
     * Creates a UserDto from a domain User.
     */
    public static UserDto fromDomain(User user) {
        return new UserDto(user.id(), user.phone().value(), user.role().name());
    }
}
