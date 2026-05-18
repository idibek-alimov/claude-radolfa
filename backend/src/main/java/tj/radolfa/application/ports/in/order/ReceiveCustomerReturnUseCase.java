package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.ReturnReason;

import java.util.List;

public interface ReceiveCustomerReturnUseCase {

    record ItemCommand(Long orderItemId, int quantity, ReturnReason reason, String notes) {}
    record Command(Long orderId, Long staffUserId, String notes, List<ItemCommand> items) {}

    CustomerReturn execute(Command command);
}
