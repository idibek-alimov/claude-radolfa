package tj.radolfa.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authentication filter for internal service-to-service clients.
 *
 * <p>Checks the {@code X-Api-Key} request header. If the value matches the
 * configured {@code radolfa.security.api-key.system-key}, the request is
 * authenticated with the {@code ADMIN} role — no OTP or JWT required.
 *
 * <p>Intended for admin tooling, automated scripts, and internal services
 * that need full platform access without user credentials.
 *
 * <p>This filter runs <em>before</em> {@link JwtAuthenticationFilter}. If
 * a valid API key is present, the JWT filter still executes but finds the
 * {@code SecurityContext} already populated and leaves it unchanged.
 */
@Component
public class ServiceApiKeyFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceApiKeyFilter.class);

    public static final String API_KEY_HEADER = "X-Api-Key";

    private final String systemKey;

    public ServiceApiKeyFilter(ApiKeyProperties properties) {
        this.systemKey = properties.systemKey();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String key = request.getHeader(API_KEY_HEADER);

        if (key != null && MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8),
                systemKey.getBytes(StandardCharsets.UTF_8))) {

            JwtAuthenticationFilter.JwtAuthenticatedUser principal =
                    new JwtAuthenticationFilter.JwtAuthenticatedUser(0L, "service@admin", "ADMIN");

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

            SecurityContextHolder.getContext().setAuthentication(auth);
            LOG.debug("[API-KEY] Service authenticated via {}", API_KEY_HEADER);
        }

        filterChain.doFilter(request, response);
    }
}
