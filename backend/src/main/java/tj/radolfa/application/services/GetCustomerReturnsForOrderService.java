package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetCustomerReturnsForOrderUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.domain.model.CustomerReturn;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetCustomerReturnsForOrderService implements GetCustomerReturnsForOrderUseCase {

    private final LoadCustomerReturnPort loadCustomerReturnPort;

    public GetCustomerReturnsForOrderService(LoadCustomerReturnPort loadCustomerReturnPort) {
        this.loadCustomerReturnPort = loadCustomerReturnPort;
    }

    @Override
    public List<CustomerReturn> execute(Long orderId) {
        return loadCustomerReturnPort.loadAllByOrderId(orderId);
    }
}
