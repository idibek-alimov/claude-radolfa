package tj.radolfa.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response body returned by the import sync endpoint.
 *
 * {@code synced} is the count of records that were successfully
 * upserted; {@code errors} is the count of records that threw
 * during processing (logged but not fatal).
 * {@code skippedReason} is present only when a record was skipped (e.g., missing user).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImportResultDto(int synced, int errors, String skippedReason) {

    public ImportResultDto(int synced, int errors) {
        this(synced, errors, null);
    }
}
