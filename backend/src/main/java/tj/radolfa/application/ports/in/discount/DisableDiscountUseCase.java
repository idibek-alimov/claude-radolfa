package tj.radolfa.application.ports.in.discount;

import tj.radolfa.domain.model.Discount;

public interface DisableDiscountUseCase {

    record Command(Long id, boolean disable) {}

    Discount execute(Command command);
}
