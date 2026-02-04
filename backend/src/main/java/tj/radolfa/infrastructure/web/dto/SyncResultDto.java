package tj.radolfa.infrastructure.web.dto;

/**
 * Response body returned by the incremental-sync endpoint.
 *
 * {@code synced} is the count of products that were successfully
 * upserted; {@code errors} is the count of snapshots that threw
 * during processing (logged but not fatal).
 */
public record SyncResultDto(int synced, int errors) {}
