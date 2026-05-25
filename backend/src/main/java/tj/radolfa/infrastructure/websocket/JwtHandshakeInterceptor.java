package tj.radolfa.infrastructure.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.security.JwtTokenExtractor;
import tj.radolfa.infrastructure.security.JwtUtil;

import java.util.Map;

/**
 * Extracts and validates the JWT during the SockJS HTTP handshake, then stores the
 * authenticated {@link JwtAuthenticatedUser} in the WebSocket session attributes.
 *
 * <p>The STOMP channel interceptor reads from these attributes on the CONNECT frame.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);
    static final String ATTR_USER = "authenticatedUser";

    private static final String CLAIM_ROLE    = "role";
    private static final String CLAIM_USER_ID = "userId";

    private final JwtTokenExtractor jwtTokenExtractor;
    private final JwtUtil           jwtUtil;
    private final LoadUserPort      loadUserPort;

    public JwtHandshakeInterceptor(JwtTokenExtractor jwtTokenExtractor,
                                   JwtUtil jwtUtil,
                                   LoadUserPort loadUserPort) {
        this.jwtTokenExtractor = jwtTokenExtractor;
        this.jwtUtil           = jwtUtil;
        this.loadUserPort      = loadUserPort;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            jwtTokenExtractor.extract(servletRequest.getServletRequest()).ifPresent(token ->
                    jwtUtil.validateAccessToken(token).ifPresent(claims -> {
                        Long   userId = claims.get(CLAIM_USER_ID, Long.class);
                        String role   = claims.get(CLAIM_ROLE, String.class);
                        String phone  = claims.getSubject();

                        if (userId == null || role == null) return;

                        loadUserPort.loadById(userId).ifPresent(user -> {
                            if (!user.enabled()) {
                                log.warn("[WS-HANDSHAKE] Rejected disabled user userId={}", userId);
                                return;
                            }
                            attributes.put(ATTR_USER, new JwtAuthenticatedUser(userId, phone, role));
                            log.debug("[WS-HANDSHAKE] Principal stored userId={} role={}", userId, role);
                        });
                    })
            );
        }
        return true; // always allow the HTTP handshake; auth is enforced at STOMP CONNECT
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}
