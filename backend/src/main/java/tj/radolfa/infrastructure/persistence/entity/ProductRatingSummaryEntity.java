package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_rating_summaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingSummaryEntity {

    @Id
    @Column(name = "listing_variant_id")
    private Long listingVariantId;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false, columnDefinition = "integer default 0")
    private int reviewCount;

    @Column(name = "count_5", nullable = false, columnDefinition = "integer default 0")
    private int count5;

    @Column(name = "count_4", nullable = false, columnDefinition = "integer default 0")
    private int count4;

    @Column(name = "count_3", nullable = false, columnDefinition = "integer default 0")
    private int count3;

    @Column(name = "count_2", nullable = false, columnDefinition = "integer default 0")
    private int count2;

    @Column(name = "count_1", nullable = false, columnDefinition = "integer default 0")
    private int count1;

    @Column(name = "size_accurate", nullable = false, columnDefinition = "integer default 0")
    private int sizeAccurate;

    @Column(name = "size_runs_small", nullable = false, columnDefinition = "integer default 0")
    private int sizeRunsSmall;

    @Column(name = "size_runs_large", nullable = false, columnDefinition = "integer default 0")
    private int sizeRunsLarge;

    @Column(name = "last_calculated_at", nullable = false)
    private Instant lastCalculatedAt;
}
