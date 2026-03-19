package tj.radolfa.infrastructure.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tj.radolfa.infrastructure.security.ApiKeyAuthenticationFilter;
import tj.radolfa.infrastructure.security.ApiKeyProperties;
import tj.radolfa.infrastructure.security.CorsProperties;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter;
import tj.radolfa.infrastructure.security.JwtProperties;
import tj.radolfa.infrastructure.security.OtpProperties;
import tj.radolfa.infrastructure.security.RateLimitProperties;

import java.util.List;

/**
 * Spring Security configuration with JWT-based authentication.
 *
 * <h3>Security Model</h3>
 * <ul>
 * <li>Stateless session policy (no server-side sessions)</li>
 * <li>JWT tokens for authentication</li>
 * <li>Role-based access control (RBAC)</li>
 * </ul>
 *
 * <h3>Role Permissions</h3>
 * <ul>
 * <li><b>USER</b>: Can view profile, wishlist, history</li>
 * <li><b>MANAGER</b>: Can upload images, edit descriptions (NOT
 * price/name/stock)</li>
 * <li><b>SYNC</b>: Can call import sync endpoints</li>
 * </ul>
 *
 * <h3>Critical Constraints (from CLAUDE.md)</h3>
 * <ul>
 * <li>ERPNext is the SOURCE OF TRUTH for price, name, stock</li>
 * <li>MANAGER role cannot change Price</li>
 * <li>SYNC role handles import synchronisation</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({
                JwtProperties.class,
                OtpProperties.class,
                CorsProperties.class,
                RateLimitProperties.class,
                ApiKeyProperties.class
})
public class SecurityConfig {

        private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final CorsProperties corsProperties;

        public SecurityConfig(ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        CorsProperties corsProperties) {
                this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.corsProperties = corsProperties;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // Disable CSRF (stateless JWT auth doesn't need it)
                                .csrf(AbstractHttpConfigurer::disable)

                                // Enable CORS with custom configuration
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // Stateless session policy
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Return 401 for missing/invalid credentials, 403 for wrong role
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((req, res, e) ->
                                                        res.sendError(401, "Unauthorized"))
                                                .accessDeniedHandler((req, res, e) ->
                                                        res.sendError(403, "Forbidden")))

                                // Add API key filter first (machine-to-machine clients, e.g. ERPNext)
                                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                                // Add JWT filter for browser / mobile clients
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                                // Authorization rules
                                .authorizeHttpRequests(requests -> requests
                                                // ============================================================
                                                // Public endpoints (no authentication required)
                                                // ============================================================
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/actuator/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/home/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/listings/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/colors").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/loyalty-tiers").permitAll()

                                                // Swagger / OpenAPI endpoints
                                                .requestMatchers("/swagger-ui/**").permitAll()
                                                .requestMatchers("/swagger-ui.html").permitAll()
                                                .requestMatchers("/v3/api-docs/**").permitAll()
                                                .requestMatchers("/v3/api-docs.yaml").permitAll()

                                                // ============================================================
                                                // SYNC role only: Import sync and Search index management
                                                // Critical: Only SYNC can modify price/name/stock
                                                // ============================================================
                                                .requestMatchers("/api/v1/sync/**").hasRole("SYNC")
                                                .requestMatchers("/api/v1/search/**").hasRole("SYNC")

                                                // ============================================================
                                                // MANAGER role: Listing enrichment (images, descriptions)
                                                // Note: MANAGER can enrich listings but NOT modify ERP fields
                                                // ============================================================
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/listings/*")
                                                .hasRole("MANAGER")
                                                .requestMatchers(HttpMethod.POST, "/api/v1/listings/*/images")
                                                .hasRole("MANAGER")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/listings/*/images")
                                                .hasRole("MANAGER")
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/colors/*")
                                                .hasRole("MANAGER")

                                                // ============================================================
                                                // USER role: Profile, wishlist, order history
                                                // ============================================================
                                                .requestMatchers("/api/v1/users/me/**")
                                                .hasAnyRole("USER", "MANAGER", "SYNC")
                                                .requestMatchers("/api/v1/wishlist/**")
                                                .hasAnyRole("USER", "MANAGER", "SYNC")
                                                .requestMatchers("/api/v1/orders/**")
                                                .hasAnyRole("USER", "MANAGER", "SYNC")

                                                // ============================================================
                                                // Default: require authentication for all other endpoints
                                                // ============================================================
                                                .anyRequest().authenticated());

                return http.build();
        }

        /**
         * CORS configuration allowing frontend origins.
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Allowed origins from configuration
                configuration.setAllowedOrigins(corsProperties.allowedOrigins());

                // Allowed methods
                configuration.setAllowedMethods(List.of(
                                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

                // Allowed headers
                configuration.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "Origin",
                                "X-Requested-With",
                                "X-Api-Key",
                                "Idempotency-Key"));

                // Exposed headers (allow frontend to read these)
                configuration.setExposedHeaders(List.of(
                                "Authorization",
                                "Content-Type"));

                // Allow credentials (cookies, auth headers)
                configuration.setAllowCredentials(true);

                // Cache preflight response for 1 hour
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", configuration);
                return source;
        }
}
