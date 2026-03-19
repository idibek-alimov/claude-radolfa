package tj.radolfa.application.ports.in.sync;

import java.math.BigDecimal;
import java.util.List;

public interface SyncOrdersUseCase {

    record SyncOrderCommand(
            String externalOrderId,
            String customerPhone,
            String status,
            BigDecimal totalAmount,
            List<SyncOrderItemCommand> items) {
    }

    record SyncOrderItemCommand(
            String skuCode,
            String productName,
            int quantity,
            BigDecimal price) {
    }

    enum SyncStatus { SYNCED, SKIPPED }

    record SyncResult(SyncStatus status, String message) {

        public static SyncResult synced(String externalOrderId) {
            return new SyncResult(SyncStatus.SYNCED, "Order synced: " + externalOrderId);
        }

        public static SyncResult skipped(String reason) {
            return new SyncResult(SyncStatus.SKIPPED, reason);
        }
    }

    SyncResult execute(SyncOrderCommand command);
}
