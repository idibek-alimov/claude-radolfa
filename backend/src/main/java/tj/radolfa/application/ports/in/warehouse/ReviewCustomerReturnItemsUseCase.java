package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.domain.model.Resellability;

import java.util.List;

public interface ReviewCustomerReturnItemsUseCase {

    record ItemReview(Long orderItemId, Resellability resellability) {}

    record Command(Long returnId, Long adminUserId, List<ItemReview> reviews) {}

    void execute(Command command);
}
