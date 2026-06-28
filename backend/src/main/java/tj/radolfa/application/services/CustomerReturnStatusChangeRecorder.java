package tj.radolfa.application.services;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.SaveCustomerReturnStatusChangePort;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.CustomerReturnStatusChange;

import java.time.Instant;

/**
 * Centralised helper for appending a {@code customer_return_status_changes} row.
 *
 * <p>Must be called inside an existing {@code @Transactional} boundary so the ledger
 * row and the status change are committed (or rolled back) together.
 */
@Component
public class CustomerReturnStatusChangeRecorder {

    private final SaveCustomerReturnStatusChangePort saveCustomerReturnStatusChangePort;

    public CustomerReturnStatusChangeRecorder(
            SaveCustomerReturnStatusChangePort saveCustomerReturnStatusChangePort) {
        this.saveCustomerReturnStatusChangePort = saveCustomerReturnStatusChangePort;
    }

    public void record(Long returnId, CustomerReturnStatus from, CustomerReturnStatus to,
                       Long actorUserId, String reason) {
        saveCustomerReturnStatusChangePort.append(
                new CustomerReturnStatusChange(null, returnId, from, to, actorUserId, reason, Instant.now()));
    }
}
