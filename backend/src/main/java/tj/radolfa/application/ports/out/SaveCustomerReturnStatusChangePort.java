package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.CustomerReturnStatusChange;

public interface SaveCustomerReturnStatusChangePort {
    CustomerReturnStatusChange append(CustomerReturnStatusChange change);
}
