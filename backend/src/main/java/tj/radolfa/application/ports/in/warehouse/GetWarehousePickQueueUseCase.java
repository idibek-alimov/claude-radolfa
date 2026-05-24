package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.application.readmodel.PickQueueItem;
import tj.radolfa.domain.model.PageResult;

public interface GetWarehousePickQueueUseCase {
    PageResult<PickQueueItem> execute(int page, int size, String search);
}
