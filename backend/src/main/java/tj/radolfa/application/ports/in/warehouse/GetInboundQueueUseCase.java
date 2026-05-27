package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.application.readmodel.InboundQueueItem;
import tj.radolfa.domain.model.PageResult;

public interface GetInboundQueueUseCase {
    PageResult<InboundQueueItem> execute(int page, int size, String search);
}
