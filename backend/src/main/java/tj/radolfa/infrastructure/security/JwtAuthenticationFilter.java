package tj.radolfa.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter that extracts and validates JWT tokens
 * from the Authorization header and sets the Spring Security context.
 *
 * <p>Expected header format: {@code Authorization: Bearer <token>}
 *
 * <p>If the token is valid, the filter sets an authentication object
 * with the user's phone as principal and role as granted authority.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USER_ID = "userId";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            authenticateFromToken(token);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Attempts to authenticate the request using the provided JWT token.
     * If successful, sets the SecurityContext with the authenticated user.
     */
    private void authenticateFromToken(String token) {
        jwtUtil.validateToken(token).ifPresent(claims -> {
            String phone = claims.getSubject();
            String role = claims.get(CLAIM_ROLE, String.class);
            Long userId = claims.get(CLAIM_USER_ID, Long.class);

            if (phone != null && role != null) {
                // Create authority with ROLE_ prefix for Spring Security
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                // Create authentication token with user details as principal
                JwtAuthenticatedUser principal = new JwtAuthenticatedUser(userId, phone, role);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
                LOG.debug("[JWT] Authenticated user: phone={}, role={}", phone, role);
            }
        });
    }

    /**
     * Principal object representing an authenticated JWT user.
     */
    public record JwtAuthenticatedUser(Long userId, String phone, String role) {}
}
