package tj.radolfa;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import tj.radolfa.infrastructure.web.SearchController;

/**
 * Radolfa – Spring Boot entry point.
 *
 * Component scanning starts from {@code tj.radolfa} and covers
 * domain, application, and infrastructure sub-packages automatically.
 */
@SpringBootApplication
@EnableScheduling
public class RadolfaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RadolfaApplication.class, args);
    }

    @Bean
    public CommandLineRunner indexSeedData(SearchController searchController) {
        return args -> {
            System.out.println("--- TEMPORARY HACK: Starting startup re-index ---");
            try {
                // --- ADDED: Set system authentication manually ---
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                "system-launcher", null,
                                java.util.List
                                        .of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                "ROLE_SYSTEM"))));
                searchController.reindex();
                System.out.println("--- TEMPORARY HACK: Startup re-index completed ---");
            } catch (Exception e) {
                System.err.println("--- TEMPORARY HACK: Startup re-index failed: " + e.getMessage());
                e.printStackTrace(); // Helpful for debugging
            } finally {
                // Clear context after use
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        };
    }
}
