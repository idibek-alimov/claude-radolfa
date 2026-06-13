package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_notification_prefs")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPrefsEntity extends BaseAuditEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "order_updates", nullable = false)
    private boolean orderUpdates;

    @Column(name = "promotions", nullable = false)
    private boolean promotions;

    @Column(name = "sms_messages", nullable = false)
    private boolean smsMessages;
}
