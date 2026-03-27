package tj.radolfa.application.ports.in.discount.type;

import tj.radolfa.domain.model.DiscountType;

public interface CreateDiscountTypeUseCase {

    record Command(String name, int rank) {}

    DiscountType execute(Command command);
}
