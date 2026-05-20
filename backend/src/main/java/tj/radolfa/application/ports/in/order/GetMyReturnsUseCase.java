package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.PageResult;

public interface GetMyReturnsUseCase {
    PageResult<CustomerReturn> execute(Long userId, int page, int size);
}
