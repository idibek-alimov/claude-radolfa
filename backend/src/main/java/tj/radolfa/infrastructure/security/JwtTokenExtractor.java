package tj.radolfa.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Shared JWT token extraction logic used by both the HTTP filter and the STOMP channel interceptor.
 *
 * <p>Extraction order: HTTP-only cookie (browser clients) → Authorization Bearer header (API clients).
 */
@Component
public class JwtTokenExtractor {

    private static final String BEARER_PREFIX = "Bearer ";

    public Optional<String> extract(HttpServletRequest request) {
        // 1. HTTP-only cookie (preferred for browser clients)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AuthCookieManager.AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        // 2. Authorization header fallback (API / mobile clients)
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return Optional.of(authHeader.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
