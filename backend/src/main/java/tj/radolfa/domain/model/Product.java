package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Core domain aggregate for a product.
 *
 * Plain Java class -- intentionally NOT a record, because ERP-locked fields
 * must be mutated in-place by {@link #enrichWithErpData}.
 *
 * STRICT RULE: zero Spring / JPA / Jackson / Lombok annotations allowed here.
 */
public class Product {

    private final Long   id;
    private final String erpId;

    // ---- ERP-locked fields (written only by enrichWithErpData) ----
    private String     name;
    private BigDecimal price;
    private Integer    stock;

    // ---- Enrichment fields (editable by the application) ----------
    private String       webDescription;
    private boolean      topSelling;
    private List<String> images;

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------
    public Product(Long id,
                   String erpId,
                   String name,
                   BigDecimal price,
                   Integer stock,
                   String webDescription,
                   boolean topSelling,
                   List<String> images) {
        this.id             = id;
        this.erpId          = erpId;
        this.name           = name;
        this.price          = price;
        this.stock          = stock;
        this.webDescription = webDescription;
        this.topSelling     = topSelling;
        this.images         = images != null ? List.copyOf(images) : List.of();
    }

    // ----------------------------------------------------------------
    // ERP Merge – the ONLY path that writes locked fields
    // ----------------------------------------------------------------

    /**
     * Overwrites ONLY the three ERP-locked fields.
     * Enrichment fields (webDescription, topSelling, images) are untouched.
     */
    public void enrichWithErpData(String name, BigDecimal price, Integer stock) {
        this.name  = name;
        this.price = price;
        this.stock = stock;
    }

    // ----------------------------------------------------------------
    // Getters (no setters – mutation is controlled)
    // ----------------------------------------------------------------
    public Long         getId()             { return id; }
    public String       getErpId()          { return erpId; }
    public String       getName()           { return name; }
    public BigDecimal   getPrice()          { return price; }
    public Integer      getStock()          { return stock; }
    public String       getWebDescription() { return webDescription; }
    public boolean      isTopSelling()      { return topSelling; }
    public List<String> getImages()         { return Collections.unmodifiableList(images); }
}
