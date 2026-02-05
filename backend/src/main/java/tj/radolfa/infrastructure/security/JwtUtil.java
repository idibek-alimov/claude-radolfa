package tj.radolfa.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tj.radolfa.domain.model.UserRole;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Utility class for JWT token generation and validation.
 *
 * <p>Tokens include the following claims:
 * <ul>
 *   <li>{@code sub} - User phone number (subject)</li>
 *   <li>{@code userId} - Database user ID</li>
 *   <li>{@code role} - User role (USER, MANAGER, SYSTEM)</li>
 *   <li>{@code iat} - Issued at timestamp</li>
 *   <li>{@code exp} - Expiration timestamp</li>
 * </ul>
 */
@Component
public class JwtUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JwtUtil.class);

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = properties.expirationMs();
    }

    /**
     * Generates a JWT token for the given user.
     *
     * @param userId the database user ID
     * @param phone  the user's phone number (used as subject)
     * @param role   the user's role
     * @return a signed JWT token string
     */
    public String generateToken(Long userId, String phone, UserRole role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(phone)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates a JWT token and extracts its claims.
     *
     * @param token the JWT token string
     * @return Optional containing claims if valid, empty if invalid/expired
     */
    public Optional<Claims> validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            LOG.debug("[JWT] Token validation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extracts the phone number (subject) from a valid token.
     *
     * @param token the JWT token string
     * @return Optional containing phone if valid, empty otherwise
     */
    public Optional<String> extractPhone(String token) {
        return validateToken(token).map(Claims::getSubject);
    }

    /**
     * Extracts the user ID from a valid token.
     *
     * @param token the JWT token string
     * @return Optional containing user ID if valid, empty otherwise
     */
    public Optional<Long> extractUserId(String token) {
        return validateToken(token)
                .map(claims -> claims.get(CLAIM_USER_ID, Long.class));
    }

    /**
     * Extracts the user role from a valid token.
     *
     * @param token the JWT token string
     * @return Optional containing role if valid, empty otherwise
     */
    public Optional<UserRole> extractRole(String token) {
        return validateToken(token)
                .map(claims -> claims.get(CLAIM_ROLE, String.class))
                .map(UserRole::valueOf);
    }
}
