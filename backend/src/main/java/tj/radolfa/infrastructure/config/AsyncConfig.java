package tj.radolfa.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async configuration for background tasks (image processing, etc.).
 *
 * <p>Thread pool sizing rationale:
 * <ul>
 *   <li>Core 2 / Max 4 — image resize is CPU-bound (Thumbnailator).
 *       More threads cause context switching, not throughput.</li>
 *   <li>Queue capacity 50 — buffers burst uploads without unbounded memory growth.</li>
 *   <li>{@link ThreadPoolExecutor.CallerRunsPolicy} — when queue is full,
 *       the request thread processes the image synchronously. Graceful
 *       degradation instead of rejection.</li>
 * </ul>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("imageProcessorExecutor")
    public TaskExecutor imageProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("img-proc-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
