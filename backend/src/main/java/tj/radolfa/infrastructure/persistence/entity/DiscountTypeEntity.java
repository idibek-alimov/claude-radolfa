package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import tj.radolfa.domain.model.StackingPolicy;

import java.time.Instant;

@Entity
@Table(name = "discount_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 64)
    private String name;

    @Column(name = "rank", nullable = false, unique = true)
    private int rank;

    @Enumerated(EnumType.STRING)
    @Column(name = "stacking_policy", nullable = false, length = 16)
    private StackingPolicy stackingPolicy = StackingPolicy.BEST_WINS;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
