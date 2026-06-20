package tj.radolfa.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CheckoutProperties.class)
public class CheckoutConfig {
}
