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
    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final long maxAgeSeconds;
    private final long refreshMaxAgeSeconds;
    private final boolean secureCookie;

    public AuthCookieManager(JwtProperties jwtProperties, Environment environment) {
        this.maxAgeSeconds = jwtProperties.expirationMs() / 1000;
        this.refreshMaxAgeSeconds = jwtProperties.refreshExpirationMs() / 1000;
        this.secureCookie = !Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    /**
     * Builds the Set-Cookie header for a successful login (access token).
     *
     * @param token the JWT access token value
     * @return a {@link ResponseCookie} with full security flags and JWT-matching maxAge
     */
    public ResponseCookie createLoginCookie(String token) {
        return buildCookie(AUTH_COOKIE_NAME, token, maxAgeSeconds, "/");
    }

    /**
     * Builds the Set-Cookie header that clears the access token cookie on logout.
     *
     * @return a {@link ResponseCookie} with empty value and maxAge=0
     */
    public ResponseCookie createLogoutCookie() {
        return buildCookie(AUTH_COOKIE_NAME, "", 0, "/");
    }

    /**
     * Builds the Set-Cookie header for the refresh token.
     * Path is restricted to {@code /api/v1/auth} so the browser only sends it on auth requests.
     *
     * @param token the JWT refresh token value
     * @return a {@link ResponseCookie} with restricted path and long maxAge
     */
    public ResponseCookie createRefreshLoginCookie(String token) {
        return buildCookie(REFRESH_COOKIE_NAME, token, refreshMaxAgeSeconds, "/api/v1/auth");
    }

    /**
     * Builds the Set-Cookie header that clears the refresh token cookie on logout.
     *
     * @return a {@link ResponseCookie} with empty value, maxAge=0, and restricted path
     */
    public ResponseCookie createRefreshLogoutCookie() {
        return buildCookie(REFRESH_COOKIE_NAME, "", 0, "/api/v1/auth");
    }

    private ResponseCookie buildCookie(String name, String value, long maxAge, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(secureCookie ? "Strict" : "Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
