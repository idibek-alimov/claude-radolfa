package tj.radolfa.infrastructure.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.util.List;

/**
 * Elasticsearch document representing a {@code ListingVariant} — the unit
 * the storefront displays as a product card.
 *
 * <p>Denormalized: carries the {@code ProductBase.name}, the variant's own
 * fields, and aggregated SKU data so the grid can be served entirely from
 * Elasticsearch without touching PostgreSQL.
 */
@Document(indexName = "listings")
@Setting(settingPath = "elasticsearch/listing-settings.json")
public class ListingDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String slug;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {
                    @InnerField(suffix = "autocomplete",
                            type = FieldType.Text,
                            analyzer = "autocomplete_analyzer",
                            searchAnalyzer = "autocomplete_search_analyzer"),
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String name;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String colorKey;

    @Field(type = FieldType.Keyword)
    private String colorHexCode;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String webDescription;

    @Field(type = FieldType.Keyword, index = false)
    private List<String> images;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Integer)
    private Integer totalStock;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant lastSyncAt;

    /** Human-friendly product code, e.g. "10047". Used for admin SKU-picker search. */
    @Field(type = FieldType.Keyword)
    private String productCode;

    /** All SKU codes belonging to this variant, e.g. ["SKU-ABC123EF456", "SKU-DEF456GH789"]. */
    @Field(type = FieldType.Keyword)
    private List<String> skuCodes;

    /** The owning ProductBase id — stored so ES search results can build Edit navigation URLs. */
    @Field(type = FieldType.Long)
    private Long productBaseId;

    /** ProductStatus enum name (e.g. "ACTIVE"). Filtered at read time. */
    @Field(type = FieldType.Keyword)
    private String status;

    /** ProductBase.categoryId — used to filter by category + descendant IDs. */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /** ProductBase.brandId (may be null). */
    @Field(type = FieldType.Long)
    private Long brandId;

    /** ProductBase.brand.name, denormalized for the brand facet (may be null). */
    @Field(type = FieldType.Keyword)
    private String brandName;

    /**
     * Snapshot of the active campaign discount percentage at index time
     * (may go stale until the next reindex; see search redesign Phase 2).
     */
    @Field(type = FieldType.Integer)
    private Integer discountPercentage;

    /** Average rating from {@code product_rating_summaries} (may be null). */
    @Field(type = FieldType.Double)
    private Double ratingAverage;

    /** ListingVariant.createdAt — used by the NEWEST sort. */
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant createdAt;

    public ListingDocument() {}

    public ListingDocument(Long id, String slug, String name, String category,
                           String colorKey, String colorHexCode,
                           String webDescription, List<String> images,
                           Double price, Integer totalStock,
                           Instant lastSyncAt,
                           String productCode, List<String> skuCodes,
                           Long productBaseId, String status,
                           Long categoryId, Long brandId, String brandName,
                           Integer discountPercentage, Double ratingAverage,
                           Instant createdAt) {
        this.id             = id;
        this.slug           = slug;
        this.name           = name;
        this.category       = category;
        this.colorKey       = colorKey;
        this.colorHexCode   = colorHexCode;
        this.webDescription = webDescription;
        this.images         = images;
        this.price          = price;
        this.totalStock     = totalStock;
        this.lastSyncAt     = lastSyncAt;
        this.productCode    = productCode;
        this.skuCodes       = skuCodes;
        this.productBaseId  = productBaseId;
        this.status         = status;
        this.categoryId     = categoryId;
        this.brandId        = brandId;
        this.brandName      = brandName;
        this.discountPercentage = discountPercentage;
        this.ratingAverage  = ratingAverage;
        this.createdAt      = createdAt;
    }

    public Long         getId()             { return id; }
    public String       getSlug()           { return slug; }
    public String       getName()           { return name; }
    public String       getCategory()       { return category; }
    public String       getColorKey()       { return colorKey; }
    public String       getColorHexCode()   { return colorHexCode; }
    public String       getWebDescription() { return webDescription; }
    public List<String> getImages()         { return images; }
    public Double       getPrice()          { return price; }
    public Integer      getTotalStock()     { return totalStock; }
    public Instant      getLastSyncAt()     { return lastSyncAt; }
    public String       getProductCode()    { return productCode; }
    public List<String> getSkuCodes()       { return skuCodes; }
    public Long         getProductBaseId()  { return productBaseId; }
    public String       getStatus()         { return status; }
    public Long         getCategoryId()         { return categoryId; }
    public Long         getBrandId()            { return brandId; }
    public String       getBrandName()          { return brandName; }
    public Integer      getDiscountPercentage() { return discountPercentage; }
    public Double       getRatingAverage()      { return ratingAverage; }
    public Instant      getCreatedAt()          { return createdAt; }
}
