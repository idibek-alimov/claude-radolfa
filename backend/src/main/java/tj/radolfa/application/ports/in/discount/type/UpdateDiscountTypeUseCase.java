package tj.radolfa.application.ports.in.discount.type;

import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.StackingPolicy;

public interface UpdateDiscountTypeUseCase {

    record Command(Long id, String name, int rank, StackingPolicy stackingPolicy) {}

    DiscountType execute(Command command);
}
