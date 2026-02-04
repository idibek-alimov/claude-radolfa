package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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

    // ---- Audit (written only by enrichWithErpData) -----------------
    private Instant lastErpSyncAt;

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
                   List<String> images,
                   Instant lastErpSyncAt) {
        this.id             = id;
        this.erpId          = erpId;
        this.name           = name;
        this.price          = price;
        this.stock          = stock;
        this.webDescription = webDescription;
        this.topSelling     = topSelling;
        this.images         = new ArrayList<>(images != null ? images : List.of());
        this.lastErpSyncAt  = lastErpSyncAt;
    }

    // ----------------------------------------------------------------
    // ERP Merge – the ONLY path that writes locked fields
    // ----------------------------------------------------------------

    /**
     * Overwrites ONLY the three ERP-locked fields and stamps the audit clock.
     * Enrichment fields (webDescription, topSelling, images) are untouched.
     */
    public void enrichWithErpData(String name, BigDecimal price, Integer stock) {
        this.name          = name;
        this.price         = price;
        this.stock         = stock;
        this.lastErpSyncAt = Instant.now();
    }

    // ----------------------------------------------------------------
    // Enrichment mutation – Radolfa-owned side
    // ----------------------------------------------------------------

    /**
     * Appends an S3 URL to the enrichment image list.
     * This is the controlled mutation path for the images field,
     * symmetric to {@link #enrichWithErpData} for ERP-locked fields.
     */
    public void addImage(String url) {
        this.images.add(url);
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
    public Instant      getLastErpSyncAt()  { return lastErpSyncAt; }
}
