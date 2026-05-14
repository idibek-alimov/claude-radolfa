package tj.radolfa.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LoyaltyRewardProperties.class)
public class LoyaltyConfig {
}
