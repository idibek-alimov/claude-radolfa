package tj.radolfa.infrastructure.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.security.JwtUtil;
import tj.radolfa.domain.model.UserRole;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates JWT authentication on STOMP CONNECT and enforces subscription-level
 * access guards for role-sensitive topics.
 *
 * <p>Auth flow:
 * <ol>
 *   <li>CONNECT — reads {@link JwtHandshakeInterceptor#ATTR_USER} from session attributes
 *       (cookie path) or falls back to the STOMP {@code Authorization} header (mobile/API clients).
 *       Sets the principal on the accessor so Spring routes {@code /user/...} correctly.</li>
 *   <li>SUBSCRIBE — asserts role matches the destination:
 *     <ul>
 *       <li>{@code /user/**} — allowed for any authenticated user (Spring routes by Principal).</li>
 *       <li>{@code /topic/pickpoint/{id}} — PICKPOINT_STAFF whose {@code pickpointId} matches.</li>
 *       <li>{@code /topic/admin/alerts} — ADMIN or MANAGER only.</li>
 *     </ul>
 *   </li>
 * </ol>
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private static final String BEARER_PREFIX     = "Bearer ";
    private static final Pattern PICKPOINT_TOPIC  = Pattern.compile("^/topic/pickpoint/(\\d+)$");
    private static final String ADMIN_ALERT_TOPIC = "/topic/admin/alerts";

    private static final String CLAIM_ROLE    = "role";
    private static final String CLAIM_USER_ID = "userId";

    private final JwtUtil      jwtUtil;
    private final LoadUserPort loadUserPort;

    public StompAuthChannelInterceptor(JwtUtil jwtUtil, LoadUserPort loadUserPort) {
        this.jwtUtil      = jwtUtil;
        this.loadUserPort = loadUserPort;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();
        if (command == null) return message;

        return switch (command) {
            case CONNECT    -> handleConnect(message, accessor);
            case SUBSCRIBE  -> handleSubscribe(message, accessor);
            default         -> message;
        };
    }

    // ── CONNECT ───────────────────────────────────────────────────────────────

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        // 1. Try principal from HandshakeInterceptor (cookie-based auth)
        Map<String, Object> attrs = accessor.getSessionAttributes();
        JwtAuthenticatedUser principal = attrs != null
                ? (JwtAuthenticatedUser) attrs.get(JwtHandshakeInterceptor.ATTR_USER)
                : null;

        // 2. Fall back: Authorization header in STOMP CONNECT frame (mobile/API clients)
        if (principal == null) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String header = authHeaders.get(0);
                if (header != null && header.startsWith(BEARER_PREFIX)) {
                    principal = validateToken(header.substring(BEARER_PREFIX.length()));
                }
            }
        }

        if (principal == null) {
            throw new MessageDeliveryException(message,
                    "WebSocket authentication required — no valid JWT found");
        }

        accessor.setUser(principal);
        log.debug("[STOMP-AUTH] CONNECT accepted userId={} role={}", principal.userId(), principal.role());
        return message;
    }

    // ── SUBSCRIBE ─────────────────────────────────────────────────────────────

    private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return message;

        JwtAuthenticatedUser user = resolveUser(accessor);

        if (destination.startsWith("/user/")) {
            // Spring routes /user/... by Principal — authenticated users may subscribe to their own queue
            if (user == null) throw new AccessDeniedException("Authentication required");
            return message;
        }

        Matcher pickpointMatcher = PICKPOINT_TOPIC.matcher(destination);
        if (pickpointMatcher.matches()) {
            if (user == null || !UserRole.PICKPOINT_STAFF.name().equals(user.role())) {
                throw new AccessDeniedException("Only PICKPOINT_STAFF may subscribe to pickpoint topics");
            }
            Long topicPickpointId = Long.parseLong(pickpointMatcher.group(1));
            Long userPickpointId  = loadUserPort.loadById(user.userId())
                    .map(u -> u.pickpointId()).orElse(null);
            if (!topicPickpointId.equals(userPickpointId)) {
                throw new AccessDeniedException("PICKPOINT_STAFF may only subscribe to their own pickpoint topic");
            }
            return message;
        }

        if (ADMIN_ALERT_TOPIC.equals(destination)) {
            if (user == null || (!UserRole.ADMIN.name().equals(user.role())
                    && !UserRole.MANAGER.name().equals(user.role()))) {
                throw new AccessDeniedException("Only ADMIN or MANAGER may subscribe to admin alerts");
            }
            return message;
        }

        throw new AccessDeniedException("Subscription to destination not permitted: " + destination);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JwtAuthenticatedUser resolveUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof JwtAuthenticatedUser u) return u;
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null && attrs.get(JwtHandshakeInterceptor.ATTR_USER) instanceof JwtAuthenticatedUser u) return u;
        return null;
    }

    private JwtAuthenticatedUser validateToken(String token) {
        return jwtUtil.validateAccessToken(token).map(claims -> {
            Long   userId = claims.get(CLAIM_USER_ID, Long.class);
            String role   = claims.get(CLAIM_ROLE, String.class);
            String phone  = claims.getSubject();
            if (userId == null || role == null) return null;
            return loadUserPort.loadById(userId)
                    .filter(u -> u.enabled())
                    .map(u -> new JwtAuthenticatedUser(userId, phone, role))
                    .orElse(null);
        }).orElse(null);
    }
}
