package tj.radolfa.domain.model;

/**
 * Root of the product hierarchy — represents an ERPNext Item Template.
 *
 * <p>A ProductBase groups all colour variants and their SKUs under a single
 * template code. The {@code name} and {@code category} fields are ERP-locked
 * and may only be updated through {@link #updateFromErp(String, String)}.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class ProductBase {

    private final Long   id;
    private final String erpTemplateCode;

    // ERP-locked fields
    private String name;
    private String category;

    public ProductBase(Long id, String erpTemplateCode, String name, String category) {
        this.id              = id;
        this.erpTemplateCode = erpTemplateCode;
        this.name            = name;
        this.category        = category;
    }

    /**
     * ERP merge — the ONLY path that writes the locked fields.
     */
    public void updateFromErp(String name, String category) {
        this.name     = name;
        this.category = category;
    }

    // ---- Getters (no setters — mutation is controlled) ----
    public Long   getId()              { return id; }
    public String getErpTemplateCode() { return erpTemplateCode; }
    public String getName()            { return name; }
    public String getCategory()        { return category; }
}
