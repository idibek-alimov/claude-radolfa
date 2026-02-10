package tj.radolfa.domain.model;

/**
 * Root of the product hierarchy — represents an ERPNext Item Template.
 *
 * <p>A ProductBase groups all colour variants and their SKUs under a single
 * template code. The {@code name} field is ERP-locked and may only be
 * updated through {@link #updateFromErp(String)}.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class ProductBase {

    private final Long   id;
    private final String erpTemplateCode;

    // ERP-locked field
    private String name;

    public ProductBase(Long id, String erpTemplateCode, String name) {
        this.id              = id;
        this.erpTemplateCode = erpTemplateCode;
        this.name            = name;
    }

    /**
     * ERP merge — the ONLY path that writes the locked {@code name} field.
     */
    public void updateFromErp(String name) {
        this.name = name;
    }

    // ---- Getters (no setters — mutation is controlled) ----
    public Long   getId()              { return id; }
    public String getErpTemplateCode() { return erpTemplateCode; }
    public String getName()            { return name; }
}
