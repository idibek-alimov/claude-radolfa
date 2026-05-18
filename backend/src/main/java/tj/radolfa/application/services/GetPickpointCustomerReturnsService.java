package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetPickpointCustomerReturnsUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.exception.PickpointAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.PageResult;

@Service
public class GetPickpointCustomerReturnsService implements GetPickpointCustomerReturnsUseCase {

    private final LoadCustomerReturnPort loadCustomerReturnPort;
    private final LoadUserPort           loadUserPort;

    public GetPickpointCustomerReturnsService(LoadCustomerReturnPort loadCustomerReturnPort,
                                               LoadUserPort loadUserPort) {
        this.loadCustomerReturnPort = loadCustomerReturnPort;
        this.loadUserPort           = loadUserPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CustomerReturn> execute(Long staffUserId, CustomerReturnStatus status, int page, int size) {
        var staff = loadUserPort.loadById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + staffUserId));

        if (staff.pickpointId() == null) {
            throw new PickpointAccessDeniedException("Staff " + staffUserId + " has no pickpoint assigned");
        }

        return loadCustomerReturnPort.loadByPickpointIdAndStatus(staff.pickpointId(), status, page, size);
    }
}
