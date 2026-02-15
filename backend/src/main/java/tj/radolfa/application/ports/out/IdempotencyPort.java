package tj.radolfa.application.ports.out;

/**
 * Out-Port: check and record idempotency keys for ERP sync events
 * to prevent duplicate processing on network retries.
 */
public interface IdempotencyPort {

    /**
     * Checks whether the given key has already been processed for the event type.
     *
     * @param key       the idempotency key from the request header
     * @param eventType the sync event type (e.g., "ORDER", "LOYALTY")
     * @return {@code true} if the key was already processed
     */
    boolean exists(String key, String eventType);

    /**
     * Records a processed idempotency key.
     *
     * @param key            the idempotency key
     * @param eventType      the sync event type
     * @param responseStatus the HTTP status code returned for this request
     */
    void save(String key, String eventType, int responseStatus);
}
