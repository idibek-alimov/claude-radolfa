package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pickpoint")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class PickpointEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "has_parking", nullable = false)
    private boolean hasParking;

    @Column(name = "has_fitting_room", nullable = false)
    private boolean hasFittingRoom;

    @Column(name = "has_card_payment", nullable = false)
    private boolean hasCardPayment;

    @Column(name = "wheelchair_accessible", nullable = false)
    private boolean wheelchairAccessible;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "temporarily_closed", nullable = false)
    private boolean temporarilyClosed;
}
