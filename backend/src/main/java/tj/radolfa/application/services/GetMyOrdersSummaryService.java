package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetMyOrdersSummaryUseCase;
import tj.radolfa.application.ports.in.order.MyOrderFilter;
import tj.radolfa.application.ports.in.order.MyOrdersSummary;
import tj.radolfa.application.ports.out.LoadOrderPort;

@Service
public class GetMyOrdersSummaryService implements GetMyOrdersSummaryUseCase {

    private final LoadOrderPort loadOrderPort;

    public GetMyOrdersSummaryService(LoadOrderPort loadOrderPort) {
        this.loadOrderPort = loadOrderPort;
    }

    @Override
    @Transactional(readOnly = true)
    public MyOrdersSummary execute(Long userId) {
        long all       = loadOrderPort.countByUserId(userId);
        long progress  = loadOrderPort.countByUserIdAndStatuses(userId, MyOrderFilter.PROGRESS.statuses());
        long delivered = loadOrderPort.countByUserIdAndStatuses(userId, MyOrderFilter.DELIVERED.statuses());
        long returns   = loadOrderPort.countByUserIdAndStatuses(userId, MyOrderFilter.RETURNS.statuses());
        return new MyOrdersSummary(all, progress, delivered, returns);
    }
}
