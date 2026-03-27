package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.UserRole;

/**
 * Out-Port: JWT token generation for authenticated users.
 * Infrastructure adapter (JwtUtil) implements this.
 */
public interface TokenIssuerPort {

    /**
     * Generates an access token for the given user.
     */
    String generateToken(Long userId, String phone, UserRole role);
}
