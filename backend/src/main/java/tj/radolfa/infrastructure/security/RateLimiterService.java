package tj.radolfa.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fixed-window rate limiter.
 *
 * Backed by {@link ConcurrentHashMap} — correct for a single-instance monolith.
 * If the service ever scales horizontally, replace with a Redis-backed implementation.
 *
 * <p>Expired windows are evicted every 5 minutes via {@link #evictExpiredWindows()}.
 */
@Component
public class RateLimiterService {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterService.class);

    private final ConcurrentHashMap<String, RateWindow> windows = new ConcurrentHashMap<>();

    private static class RateWindow {
        int count;
        final long windowStartNs;
        final long windowDurationNs;

        RateWindow(long windowStartNs, long windowDurationNs) {
            this.count = 1;
            this.windowStartNs = windowStartNs;
            this.windowDurationNs = windowDurationNs;
        }

        boolean isExpired(long now) {
            return now - windowStartNs >= windowDurationNs;
        }
    }

    /**
     * Attempts to consume a token for the given key.
     *
     * @param key         unique identifier (e.g. "login:phone:+992*****12")
     * @param maxRequests maximum allowed requests within the window
     * @param window      window duration
     * @return {@code true} if the request is allowed, {@code false} if rate-limited
     */
    public boolean tryConsume(String key, int maxRequests, Duration window) {
        long windowNs = window.toNanos();
        long now = System.nanoTime();
        int[] captured = {0};

        windows.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                var rw = new RateWindow(now, windowNs);
                captured[0] = rw.count;
                return rw;
            }
            existing.count++;
            captured[0] = existing.count;
            return existing;
        });

        return captured[0] <= maxRequests;
    }

    /**
     * Evicts expired windows every 5 minutes to prevent unbounded memory growth.
     */
    @Scheduled(fixedRate = 300_000)
    void evictExpiredWindows() {
        long now = System.nanoTime();
        int before = windows.size();
        windows.entrySet().removeIf(e -> e.getValue().isExpired(now));
        int evicted = before - windows.size();
        if (evicted > 0) {
            LOG.debug("[RATE_LIMIT] Evicted {} expired windows, {} remaining", evicted, windows.size());
        }
    }
}
