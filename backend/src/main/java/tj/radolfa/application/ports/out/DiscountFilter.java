package tj.radolfa.application.ports.out;

import java.time.LocalDate;

/**
 * Filter parameters for the admin discount list.
 * All fields are optional (null = no filter applied for that field).
 *
 * <p>status values: ACTIVE, SCHEDULED, EXPIRED, DISABLED
 */
public record DiscountFilter(Long typeId, String status, LocalDate from, LocalDate to, String search) {

    public static DiscountFilter empty() {
        return new DiscountFilter(null, null, null, null, null);
    }
}
