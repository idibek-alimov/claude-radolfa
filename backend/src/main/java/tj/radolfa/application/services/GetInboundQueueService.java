package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.GetInboundQueueUseCase;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.readmodel.InboundQueueItem;
import tj.radolfa.domain.model.PageResult;

@Service
@Transactional(readOnly = true)
public class GetInboundQueueService implements GetInboundQueueUseCase {

    private final InventoryPlacementPort placementPort;

    public GetInboundQueueService(InventoryPlacementPort placementPort) {
        this.placementPort = placementPort;
    }

    @Override
    public PageResult<InboundQueueItem> execute(int page, int size, String search) {
        return placementPort.findInboundQueue(page, size, search);
    }
}
