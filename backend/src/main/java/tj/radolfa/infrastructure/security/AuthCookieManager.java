package tj.radolfa.infrastructure.security;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Single owner of auth cookie construction and configuration.
 *
 * <p>Centralises the security-sensitive cookie flags (HttpOnly, Secure,
 * SameSite, Path) so they cannot drift between login and logout paths.
 *
 * <p>Also owns the {@link #AUTH_COOKIE_NAME} constant, referenced by
 * both the write side ({@link tj.radolfa.infrastructure.web.AuthController})
 * and the read side ({@link JwtAuthenticationFilter}).
 */
@Component
public class AuthCookieManager {

    public static final String AUTH_COOKIE_NAME = "auth_token";

    private final long maxAgeSeconds;
    private final boolean secureCookie;

    public AuthCookieManager(JwtProperties jwtProperties, Environment environment) {
        this.maxAgeSeconds = jwtProperties.expirationMs() / 1000;
        this.secureCookie = !Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    /**
     * Builds the Set-Cookie header for a successful login.
     *
     * @param token the JWT token value
     * @return a {@link ResponseCookie} with full security flags and JWT-matching maxAge
     */
    public ResponseCookie createLoginCookie(String token) {
        return buildCookie(token, maxAgeSeconds);
    }

    /**
     * Builds the Set-Cookie header that clears the auth cookie on logout.
     *
     * @return a {@link ResponseCookie} with empty value and maxAge=0
     */
    public ResponseCookie createLogoutCookie() {
        return buildCookie("", 0);
    }

    private ResponseCookie buildCookie(String value, long maxAge) {
        return ResponseCookie.from(AUTH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(secureCookie ? "Strict" : "Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
