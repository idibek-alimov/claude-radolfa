package tj.radolfa.domain.model;

/**
 * Root of the product hierarchy — represents an Item Template from the
 * authoritative external catalogue.
 *
 * <p>A ProductBase groups all colour variants and their SKUs under a single
 * external reference code. The {@code name} and {@code category} fields are
 * authoritative-source-locked and may only be updated through
 * {@link #applyExternalUpdate(String, String)}.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class ProductBase {

    private final Long   id;
    private final String externalRef;

    // Authoritative-source-locked fields
    private String name;
    private String category;

    /**
     * @param id           database PK ({@code null} for unsaved instances)
     * @param externalRef  required — external template identity, must not be blank
     * @param name         nullable — populated by import sync
     * @param category     nullable — populated by import sync
     */
    public ProductBase(Long id, String externalRef, String name, String category) {
        if (externalRef == null || externalRef.isBlank()) {
            throw new IllegalArgumentException("externalRef must not be blank");
        }
        this.id          = id;
        this.externalRef = externalRef;
        this.name        = name;
        this.category    = category;
    }

    /**
     * Authoritative-source merge — the ONLY path that writes the locked fields.
     */
    public void applyExternalUpdate(String name, String category) {
        this.name     = name;
        this.category = category;
    }

    // ---- Getters (no setters — mutation is controlled) ----
    public Long   getId()          { return id; }
    public String getExternalRef() { return externalRef; }
    public String getName()        { return name; }
    public String getCategory()    { return category; }
}
