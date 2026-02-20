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
import java.util.List;

/**
 * Authentication filter for machine-to-machine clients (e.g. ERPNext).
 *
 * <p>Checks the {@code X-Api-Key} request header. If the value matches the
 * configured {@code radolfa.security.api-key.system-key}, the request is
 * authenticated with the {@code SYSTEM} role — no OTP or JWT required.
 *
 * <p>This filter runs <em>before</em> {@link JwtAuthenticationFilter}. If
 * a valid API key is present, the JWT filter still executes but finds the
 * {@code SecurityContext} already populated and leaves it unchanged.
 *
 * <h3>ERPNext usage</h3>
 * <pre>
 * headers = {"X-Api-Key": frappe.get_doc("App Settings").system_api_key}
 * </pre>
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    public static final String API_KEY_HEADER = "X-Api-Key";

    private final String systemKey;

    public ApiKeyAuthenticationFilter(ApiKeyProperties properties) {
        this.systemKey = properties.systemKey();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String key = request.getHeader(API_KEY_HEADER);

        if (key != null && key.equals(systemKey)) {
            // Use a stable synthetic identity so audit logs in ErpSyncController remain readable
            JwtAuthenticationFilter.JwtAuthenticatedUser principal =
                    new JwtAuthenticationFilter.JwtAuthenticatedUser(0L, "erp@system", "SYSTEM");

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, null,
                    List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")));

            SecurityContextHolder.getContext().setAuthentication(auth);
            LOG.debug("[API-KEY] SYSTEM authenticated via {}", API_KEY_HEADER);
        }

        filterChain.doFilter(request, response);
    }
}
