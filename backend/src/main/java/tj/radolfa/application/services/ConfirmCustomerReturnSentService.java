package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ConfirmCustomerReturnSentUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveCustomerReturnPort;
import tj.radolfa.domain.exception.PickpointAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.CustomerReturnStatus;

@Service
public class ConfirmCustomerReturnSentService implements ConfirmCustomerReturnSentUseCase {

    private final LoadCustomerReturnPort loadCustomerReturnPort;
    private final SaveCustomerReturnPort saveCustomerReturnPort;
    private final LoadUserPort           loadUserPort;

    public ConfirmCustomerReturnSentService(LoadCustomerReturnPort loadCustomerReturnPort,
                                             SaveCustomerReturnPort saveCustomerReturnPort,
                                             LoadUserPort loadUserPort) {
        this.loadCustomerReturnPort = loadCustomerReturnPort;
        this.saveCustomerReturnPort = saveCustomerReturnPort;
        this.loadUserPort           = loadUserPort;
    }

    @Override
    @Transactional
    public void execute(Long returnId, Long staffUserId) {
        var customerReturn = loadCustomerReturnPort.loadById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerReturn not found: " + returnId));

        if (customerReturn.getStatus() != CustomerReturnStatus.RECEIVED) {
            throw new IllegalStateException(
                    "CustomerReturn " + returnId + " is not RECEIVED: " + customerReturn.getStatus());
        }

        var staff = loadUserPort.loadById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + staffUserId));

        if (staff.pickpointId() == null || !staff.pickpointId().equals(customerReturn.getPickpointId())) {
            throw new PickpointAccessDeniedException(
                    "Staff " + staffUserId + " is not assigned to pickpoint " + customerReturn.getPickpointId());
        }

        customerReturn.markSentToWarehouse(staffUserId);
        saveCustomerReturnPort.save(customerReturn);
    }
}
