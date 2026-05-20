package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "payment_saga_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSagaLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_transaction_id", nullable = false, length = 128)
    private String providerTransactionId;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;
}
