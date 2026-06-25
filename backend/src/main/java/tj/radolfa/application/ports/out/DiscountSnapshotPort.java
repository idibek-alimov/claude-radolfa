package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Discount;

public interface DiscountSnapshotPort {
    String toJson(Discount discount);
}
