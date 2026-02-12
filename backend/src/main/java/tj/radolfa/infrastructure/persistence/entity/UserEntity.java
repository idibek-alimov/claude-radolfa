package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * JPA persistence model for the {@code users} table.
 *
 * Extends {@link BaseAuditEntity} for optimistic locking ({@code @Version})
 * and standardised {@code created_at}/{@code updated_at} timestamps.
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", nullable = false, unique = true, length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private tj.radolfa.domain.model.UserRole role;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Column(name = "loyalty_points", nullable = false)
    private int loyaltyPoints;
}
