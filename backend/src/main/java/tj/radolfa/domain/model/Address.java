package tj.radolfa.domain.model;

import java.util.Objects;

/**
 * A customer's saved delivery address.
 *
 * <p>Mutable — details and the default flag are updated via named domain methods.
 *
 * <p>Pure Java — zero framework dependencies.
 */
public class Address {

    private final Long   id;
    private final Long   userId;
    private       AddressLabel label;
    private       String recipientName;
    private       String phone;
    private       String line1;
    private       String city;
    private       String postalCode;  // nullable
    private       String country;
    private       boolean isDefault;

    public Address(Long id,
                    Long userId,
                    AddressLabel label,
                    String recipientName,
                    String phone,
                    String line1,
                    String city,
                    String postalCode,
                    String country,
                    boolean isDefault) {

        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(label, "label must not be null");
        requireNonBlank(recipientName, "recipientName");
        requireNonBlank(phone, "phone");
        requireNonBlank(line1, "line1");
        requireNonBlank(city, "city");
        requireNonBlank(country, "country");

        this.id            = id;
        this.userId        = userId;
        this.label         = label;
        this.recipientName = recipientName;
        this.phone         = phone;
        this.line1         = line1;
        this.city          = city;
        this.postalCode    = postalCode;
        this.country       = country;
        this.isDefault     = isDefault;
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    // ── Domain mutations ──────────────────────────────────────────────────────

    /** Updates the editable fields of this address. The default flag is managed separately. */
    public void updateDetails(AddressLabel label,
                               String recipientName,
                               String phone,
                               String line1,
                               String city,
                               String postalCode,
                               String country) {
        Objects.requireNonNull(label, "label must not be null");
        requireNonBlank(recipientName, "recipientName");
        requireNonBlank(phone, "phone");
        requireNonBlank(line1, "line1");
        requireNonBlank(city, "city");
        requireNonBlank(country, "country");

        this.label         = label;
        this.recipientName = recipientName;
        this.phone         = phone;
        this.line1         = line1;
        this.city          = city;
        this.postalCode    = postalCode;
        this.country       = country;
    }

    /** Marks this address as the user's default. */
    public void markDefault() {
        this.isDefault = true;
    }

    /** Clears the default flag on this address. */
    public void clearDefault() {
        this.isDefault = false;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long    getId()            { return id; }
    public Long    getUserId()        { return userId; }
    public AddressLabel getLabel()    { return label; }
    public String  getRecipientName() { return recipientName; }
    public String  getPhone()         { return phone; }
    public String  getLine1()         { return line1; }
    public String  getCity()          { return city; }
    public String  getPostalCode()    { return postalCode; }
    public String  getCountry()       { return country; }
    public boolean isDefault()        { return isDefault; }
}
