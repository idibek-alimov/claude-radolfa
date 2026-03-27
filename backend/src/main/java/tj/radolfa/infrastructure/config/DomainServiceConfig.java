package tj.radolfa.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tj.radolfa.domain.service.LoyaltyCalculator;

/**
 * Exposes pure domain services as Spring beans so they can be injected
 * into application-layer services without adding Spring annotations to
 * the domain layer.
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public LoyaltyCalculator loyaltyCalculator() {
        return new LoyaltyCalculator();
    }
}
