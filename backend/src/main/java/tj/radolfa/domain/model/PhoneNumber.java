package tj.radolfa.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a validated, normalised phone number.
 *
 * <p>Pure Java — zero framework dependencies. Immutable.
 *
 * <p>Normalisation (applied in the constructor):
 * <ul>
 *   <li>All whitespace is stripped.</li>
 * </ul>
 *
 * <p>Validation:
 * <ul>
 *   <li>Must match {@code ^\+?[0-9]{9,15}$} after normalisation.</li>
 * </ul>
 */
public record PhoneNumber(String value) {

    private static final Pattern VALID_PATTERN = Pattern.compile("^\\+?[0-9]{9,15}$");

    public PhoneNumber {
        Objects.requireNonNull(value, "Phone number cannot be null");
        value = value.replaceAll("\\s+", "").trim();
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid phone number: " + value);
        }
    }

    /**
     * Factory method. Returns {@code null} when the input is null,
     * a validated {@code PhoneNumber} otherwise.
     */
    public static PhoneNumber of(String raw) {
        return raw != null ? new PhoneNumber(raw) : null;
    }

    @Override
    public String toString() {
        return value;
    }
}
