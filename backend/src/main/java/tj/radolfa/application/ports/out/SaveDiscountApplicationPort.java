package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.DiscountApplication;

public interface SaveDiscountApplicationPort {
    DiscountApplication save(DiscountApplication application);
}
