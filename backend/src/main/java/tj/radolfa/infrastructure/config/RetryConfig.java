package tj.radolfa.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate deliveryCodeRetryTemplate() {
        ExponentialBackOffPolicy backoff = new ExponentialBackOffPolicy();
        backoff.setInitialInterval(60_000L);        // 1 minute
        backoff.setMultiplier(5.0);                 // 1m → 5m → 25m
        backoff.setMaxInterval(15 * 60_000L);       // hard cap at 15 minutes

        SimpleRetryPolicy policy = new SimpleRetryPolicy(3);

        RetryTemplate template = new RetryTemplate();
        template.setBackOffPolicy(backoff);
        template.setRetryPolicy(policy);
        return template;
    }
}
