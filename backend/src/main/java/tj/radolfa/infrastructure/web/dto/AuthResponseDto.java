package tj.radolfa.infrastructure.web.dto;

/**
 * DTO for authentication response containing JWT token and user info.
 */
public record AuthResponseDto(
        String token,
        String tokenType,
        UserDto user
) {
    /**
     * Creates a Bearer token response.
     */
    public static AuthResponseDto bearer(String token, UserDto user) {
        return new AuthResponseDto(token, "Bearer", user);
    }
}
