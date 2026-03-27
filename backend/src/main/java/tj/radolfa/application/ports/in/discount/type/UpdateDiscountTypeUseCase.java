package tj.radolfa.application.ports.in.discount.type;

import tj.radolfa.domain.model.DiscountType;

public interface UpdateDiscountTypeUseCase {

    record Command(Long id, String name, int rank) {}

    DiscountType execute(Command command);
}
