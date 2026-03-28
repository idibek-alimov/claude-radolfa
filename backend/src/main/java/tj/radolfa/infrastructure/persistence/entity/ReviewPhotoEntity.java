package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_photos")
@Data
@NoArgsConstructor
public class ReviewPhotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private ReviewEntity review;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public ReviewPhotoEntity(ReviewEntity review, String url, int sortOrder) {
        this.review    = review;
        this.url       = url;
        this.sortOrder = sortOrder;
    }
}
