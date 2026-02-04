package tj.radolfa.infrastructure.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal Spring Security configuration – Phase 2 placeholder.
 *
 * <ul>
 *   <li>The sync endpoint ({@code POST /api/v1/sync/**}) is permitted at the
 *       HTTP level; the real SYSTEM-role guard lives in the controller header
 *       check and will be replaced by proper JWT-based auth in Phase 5.</li>
 *   <li>Actuator endpoints are open (internal network only in production).</li>
 *   <li>CSRF is disabled because the sync endpoint is a machine-to-machine
 *       webhook; browser-facing CSRF will be re-enabled when the frontend
 *       session auth is wired.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/api/v1/sync/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().permitAll()   // TODO: tighten in Phase 5
                );
        return http.build();
    }
}
