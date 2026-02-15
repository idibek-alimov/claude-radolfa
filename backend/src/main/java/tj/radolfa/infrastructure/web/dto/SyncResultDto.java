package tj.radolfa.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response body returned by the incremental-sync endpoint.
 *
 * {@code synced} is the count of products that were successfully
 * upserted; {@code errors} is the count of snapshots that threw
 * during processing (logged but not fatal).
 * {@code skippedReason} is present only when an order was skipped (e.g., missing user).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SyncResultDto(int synced, int errors, String skippedReason) {

    public SyncResultDto(int synced, int errors) {
        this(synced, errors, null);
    }
}
