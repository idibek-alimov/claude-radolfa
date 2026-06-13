package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tj.radolfa.domain.model.AddressLabel;

@Entity
@Table(name = "address_book")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class AddressEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "label", nullable = false, length = 16)
    private AddressLabel label;

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Column(name = "line1", nullable = false)
    private String line1;

    @Column(name = "city", nullable = false, length = 120)
    private String city;

    @Column(name = "postal_code", length = 32)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 80)
    private String country;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;
}
