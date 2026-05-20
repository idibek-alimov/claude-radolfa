package tj.radolfa.domain.model;

import java.time.Instant;

public record PaymentSagaLog(
        Long id,
        String providerTransactionId,
        String stepName,
        String outcome,
        String errorMessage,
        Instant executedAt
) {
    public static PaymentSagaLog success(String txId, String stepName) {
        return new PaymentSagaLog(null, txId, stepName, "SUCCESS", null, Instant.now());
    }

    public static PaymentSagaLog failed(String txId, String stepName, String err) {
        return new PaymentSagaLog(null, txId, stepName, "FAILED", err, Instant.now());
    }

    public static PaymentSagaLog compensated(String txId, String stepName, String err) {
        return new PaymentSagaLog(null, txId, stepName, "COMPENSATED", err, Instant.now());
    }
}
