package tj.radolfa.domain.model;

/**
 * A single delivery address — a mutable entity within the {@link AddressBook} aggregate.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class Address {

    private Long id;          // null for new (not yet persisted)
    private String label;     // "Home", "Work", "Other"
    private String recipientName;
    private String phone;
    private String street;
    private String city;
    private String region;
    private String country;
    private boolean isDefault;

    public Address(Long id,
                   String label,
                   String recipientName,
                   String phone,
                   String street,
                   String city,
                   String region,
                   String country,
                   boolean isDefault) {
        this.id = id;
        this.label = label;
        this.recipientName = recipientName;
        this.phone = phone;
        this.street = street;
        this.city = city;
        this.region = region;
        this.country = country;
        this.isDefault = isDefault;
    }

    // ---- Getters ----
    public Long    getId()            { return id; }
    public String  getLabel()         { return label; }
    public String  getRecipientName() { return recipientName; }
    public String  getPhone()         { return phone; }
    public String  getStreet()        { return street; }
    public String  getCity()          { return city; }
    public String  getRegion()        { return region; }
    public String  getCountry()       { return country; }
    public boolean isDefault()        { return isDefault; }

    // ---- Setters (package-private; mutations go through AddressBook) ----
    void setId(Long id)                     { this.id = id; }
    void setLabel(String label)             { this.label = label; }
    void setRecipientName(String name)      { this.recipientName = name; }
    void setPhone(String phone)             { this.phone = phone; }
    void setStreet(String street)           { this.street = street; }
    void setCity(String city)               { this.city = city; }
    void setRegion(String region)           { this.region = region; }
    void setCountry(String country)         { this.country = country; }
    void setDefault(boolean isDefault)      { this.isDefault = isDefault; }
}
