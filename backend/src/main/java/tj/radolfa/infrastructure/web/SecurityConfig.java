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
import tj.radolfa.infrastructure.security.ServiceApiKeyFilter;
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
 * <li><b>USER</b>: Can view profile, cart, order history</li>
 * <li><b>MANAGER</b>: Can upload images, edit descriptions (NOT price/stock)</li>
 * <li><b>ADMIN</b>: Full platform access — price, stock, orders, user management</li>
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

        private final ServiceApiKeyFilter serviceApiKeyFilter;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final CorsProperties corsProperties;

        public SecurityConfig(ServiceApiKeyFilter serviceApiKeyFilter,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        CorsProperties corsProperties) {
                this.serviceApiKeyFilter = serviceApiKeyFilter;
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

                                // Add API key filter first (service-to-service clients)
                                .addFilterBefore(serviceApiKeyFilter, UsernamePasswordAuthenticationFilter.class)

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
                                                .requestMatchers(HttpMethod.GET, "/api/v1/tags").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/loyalty-tiers").permitAll()
                                                .requestMatchers("/api/v1/webhooks/**").permitAll()

                                                // Swagger / OpenAPI endpoints
                                                .requestMatchers("/swagger-ui/**").permitAll()
                                                .requestMatchers("/swagger-ui.html").permitAll()
                                                .requestMatchers("/v3/api-docs/**").permitAll()
                                                .requestMatchers("/v3/api-docs.yaml").permitAll()

                                                // ============================================================
                                                // ADMIN only: Search index management
                                                // ============================================================
                                                .requestMatchers("/api/v1/search/**").hasRole("ADMIN")

                                                // ============================================================
                                                // ADMIN role: price, stock, order management, full admin access
                                                // ============================================================
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/admin/skus/*/price").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/admin/skus/*/stock").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/categories/*").hasRole("ADMIN")

                                                // ============================================================
                                                // ADMIN only: discount type management
                                                // ============================================================
                                                .requestMatchers("/api/v1/admin/discount-types/**").hasRole("ADMIN")

                                                // ============================================================
                                                // ADMIN only: blueprint management
                                                // ============================================================
                                                .requestMatchers(HttpMethod.POST, "/api/v1/admin/categories/*/blueprint").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/categories/*/blueprint/*").hasRole("ADMIN")

                                                // ============================================================
                                                // ADMIN only: tag management
                                                // ============================================================
                                                .requestMatchers(HttpMethod.POST, "/api/v1/admin/tags").hasRole("ADMIN")

                                                // ============================================================
                                                // MANAGER + ADMIN: product creation, content enrichment, discounts
                                                // ============================================================
                                                .requestMatchers("/api/v1/admin/**").hasAnyRole("MANAGER", "ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/listings/*")
                                                .hasAnyRole("MANAGER", "ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/v1/listings/*/images")
                                                .hasAnyRole("MANAGER", "ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/listings/*/images")
                                                .hasAnyRole("MANAGER", "ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/colors/*")
                                                .hasAnyRole("MANAGER", "ADMIN")

                                                // ============================================================
                                                // Loyalty tier management
                                                // ============================================================
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/users/*/loyalty-permanent")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/users/*/tier")
                                                .hasAnyRole("MANAGER", "ADMIN")

                                                // ============================================================
                                                // USER role: Cart, profile, wishlist, order history
                                                // ============================================================
                                                .requestMatchers("/api/v1/cart/**")
                                                .hasAnyRole("USER", "MANAGER", "ADMIN")
                                                .requestMatchers("/api/v1/users/me/**")
                                                .hasAnyRole("USER", "MANAGER", "ADMIN")
                                                .requestMatchers("/api/v1/wishlist/**")
                                                .hasAnyRole("USER", "MANAGER", "ADMIN")

                                                // ADMIN only: order status transitions
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/*/status")
                                                .hasRole("ADMIN")
                                                // USER + ADMIN: checkout, cancel, order history
                                                .requestMatchers("/api/v1/orders/**")
                                                .hasAnyRole("USER", "MANAGER", "ADMIN")

                                                // ============================================================
                                                // Payments
                                                // ============================================================
                                                // ADMIN only: refunds
                                                .requestMatchers(HttpMethod.POST, "/api/v1/payments/*/refund")
                                                .hasRole("ADMIN")
                                                // USER: initiate payment + check status
                                                .requestMatchers("/api/v1/payments/**")
                                                .hasAnyRole("USER", "MANAGER", "ADMIN")

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
                                "X-Api-Key"));

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
