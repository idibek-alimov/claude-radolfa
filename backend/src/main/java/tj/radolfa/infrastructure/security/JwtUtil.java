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
 * <p>Supports two token types differentiated by a {@code type} claim:
 * <ul>
 *   <li><b>access</b> — short-lived (15 min), contains userId, phone, role</li>
 *   <li><b>refresh</b> — long-lived (7 days), contains only userId</li>
 * </ul>
 */
@Component
public class JwtUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JwtUtil.class);

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = properties.expirationMs();
        this.refreshExpirationMs = properties.refreshExpirationMs();
    }

    /**
     * Generates an access JWT token for the given user.
     *
     * @param userId the database user ID
     * @param phone  the user's phone number (used as subject)
     * @param role   the user's role
     * @return a signed JWT access token string
     */
    public String generateToken(Long userId, String phone, UserRole role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(phone)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generates a refresh JWT token containing only the user ID.
     * Role is intentionally omitted — it will be loaded fresh from DB on refresh.
     *
     * @param userId the database user ID
     * @return a signed JWT refresh token string
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates a token and asserts it is an access token.
     *
     * @param token the JWT token string
     * @return Optional containing claims if valid access token, empty otherwise
     */
    public Optional<Claims> validateAccessToken(String token) {
        return parseToken(token).filter(claims ->
                TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class)));
    }

    /**
     * Validates a token and asserts it is a refresh token.
     *
     * @param token the JWT token string
     * @return Optional containing the userId if valid refresh token, empty otherwise
     */
    public Optional<Long> validateRefreshToken(String token) {
        return parseToken(token)
                .filter(claims -> TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class)))
                .map(claims -> claims.get(CLAIM_USER_ID, Long.class));
    }

    /**
     * Extracts the phone number (subject) from a valid token.
     */
    public Optional<String> extractPhone(String token) {
        return parseToken(token).map(Claims::getSubject);
    }

    /**
     * Extracts the user ID from a valid token.
     */
    public Optional<Long> extractUserId(String token) {
        return parseToken(token)
                .map(claims -> claims.get(CLAIM_USER_ID, Long.class));
    }

    /**
     * Extracts the user role from a valid token.
     */
    public Optional<UserRole> extractRole(String token) {
        return parseToken(token)
                .map(claims -> claims.get(CLAIM_ROLE, String.class))
                .map(UserRole::valueOf);
    }

    private Optional<Claims> parseToken(String token) {
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
}
