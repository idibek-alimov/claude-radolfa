package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.order.GetAdminOrderSummaryUseCase;
import tj.radolfa.application.ports.out.AdminOrderSummary;
import tj.radolfa.application.ports.out.AdminOrderSummaryPort;

@Service
public class GetAdminOrderSummaryService implements GetAdminOrderSummaryUseCase {

    private final AdminOrderSummaryPort port;

    public GetAdminOrderSummaryService(AdminOrderSummaryPort port) {
        this.port = port;
    }

    @Override
    public AdminOrderSummary execute() {
        return port.load();
    }
}
