package tj.radolfa.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for CORS settings.
 * Bound to {@code radolfa.security.cors.*} in application.yml.
 */
@ConfigurationProperties(prefix = "radolfa.security.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {}
