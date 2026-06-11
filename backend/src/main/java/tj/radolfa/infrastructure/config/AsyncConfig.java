package tj.radolfa.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables {@code @Async} processing for background work that must not block
 * request threads — notably Elasticsearch reindexing triggered by discount
 * campaign changes (see {@link tj.radolfa.infrastructure.search.DiscountReindexListener}).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Bounded executor for search-index work. A small pool with a generous queue
     * is sufficient: indexing is not latency-sensitive, and this prevents a burst
     * of large category-wide campaigns from spawning unbounded threads.
     */
    @Bean(name = "indexingExecutor")
    public Executor indexingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("es-index-");
        executor.initialize();
        return executor;
    }
}
