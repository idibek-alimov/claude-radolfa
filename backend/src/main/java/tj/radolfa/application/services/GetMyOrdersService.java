package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.GetMyOrdersUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.model.Order;

import java.util.List;

@Service
public class GetMyOrdersService implements GetMyOrdersUseCase {

    private final LoadOrderPort loadOrderPort;

    public GetMyOrdersService(LoadOrderPort loadOrderPort) {
        this.loadOrderPort = loadOrderPort;
    }

    @Override
    public List<Order> execute(Long userId) {
        return loadOrderPort.loadByUserId(userId);
    }
}
