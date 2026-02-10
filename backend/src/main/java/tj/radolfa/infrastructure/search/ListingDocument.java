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
    private String colorKey;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String webDescription;

    @Field(type = FieldType.Keyword, index = false)
    private List<String> images;

    @Field(type = FieldType.Double)
    private Double priceStart;

    @Field(type = FieldType.Double)
    private Double priceEnd;

    @Field(type = FieldType.Integer)
    private Integer totalStock;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant lastSyncAt;

    public ListingDocument() {}

    public ListingDocument(Long id, String slug, String name, String colorKey,
                           String webDescription, List<String> images,
                           Double priceStart, Double priceEnd,
                           Integer totalStock, Instant lastSyncAt) {
        this.id             = id;
        this.slug           = slug;
        this.name           = name;
        this.colorKey       = colorKey;
        this.webDescription = webDescription;
        this.images         = images;
        this.priceStart     = priceStart;
        this.priceEnd       = priceEnd;
        this.totalStock     = totalStock;
        this.lastSyncAt     = lastSyncAt;
    }

    public Long         getId()             { return id; }
    public String       getSlug()           { return slug; }
    public String       getName()           { return name; }
    public String       getColorKey()       { return colorKey; }
    public String       getWebDescription() { return webDescription; }
    public List<String> getImages()         { return images; }
    public Double       getPriceStart()     { return priceStart; }
    public Double       getPriceEnd()       { return priceEnd; }
    public Integer      getTotalStock()     { return totalStock; }
    public Instant      getLastSyncAt()     { return lastSyncAt; }
}
