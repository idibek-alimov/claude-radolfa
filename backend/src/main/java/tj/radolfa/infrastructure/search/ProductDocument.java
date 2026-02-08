package tj.radolfa.infrastructure.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.util.List;

/**
 * Elasticsearch document representing a product in the search index.
 */
@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/product-settings.json")
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String erpId;

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

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String webDescription;

    @Field(type = FieldType.Boolean)
    private boolean topSelling;

    @Field(type = FieldType.Keyword, index = false)
    private List<String> images;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant lastErpSyncAt;

    public ProductDocument() {
    }

    public ProductDocument(Long id, String erpId, String name, Double price,
                           Integer stock, String webDescription, boolean topSelling,
                           List<String> images, Instant lastErpSyncAt) {
        this.id = id;
        this.erpId = erpId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.webDescription = webDescription;
        this.topSelling = topSelling;
        this.images = images;
        this.lastErpSyncAt = lastErpSyncAt;
    }

    public Long getId() { return id; }
    public String getErpId() { return erpId; }
    public String getName() { return name; }
    public Double getPrice() { return price; }
    public Integer getStock() { return stock; }
    public String getWebDescription() { return webDescription; }
    public boolean isTopSelling() { return topSelling; }
    public List<String> getImages() { return images; }
    public Instant getLastErpSyncAt() { return lastErpSyncAt; }
}
