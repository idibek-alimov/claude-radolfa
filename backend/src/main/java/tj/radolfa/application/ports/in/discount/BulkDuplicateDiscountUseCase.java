package tj.radolfa.application.ports.in.discount;

import tj.radolfa.domain.model.Discount;

import java.util.List;

public interface BulkDuplicateDiscountUseCase {
    record Command(List<Long> ids) {}
    List<Discount> execute(Command command);
}
