package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.CustomerReturn;

public interface SaveCustomerReturnPort {
    CustomerReturn save(CustomerReturn customerReturn);
}
