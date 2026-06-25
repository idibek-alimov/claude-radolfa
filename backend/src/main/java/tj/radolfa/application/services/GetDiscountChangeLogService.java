package tj.radolfa.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.GetDiscountChangeLogUseCase;
import tj.radolfa.application.ports.out.LoadDiscountChangePort;
import tj.radolfa.domain.model.DiscountChange;

@Service
@Transactional(readOnly = true)
public class GetDiscountChangeLogService implements GetDiscountChangeLogUseCase {

    private final LoadDiscountChangePort loadDiscountChangePort;

    public GetDiscountChangeLogService(LoadDiscountChangePort loadDiscountChangePort) {
        this.loadDiscountChangePort = loadDiscountChangePort;
    }

    @Override
    public Page<DiscountChange> execute(Long discountId, Pageable pageable) {
        return loadDiscountChangePort.findByDiscountId(discountId, pageable);
    }
}
