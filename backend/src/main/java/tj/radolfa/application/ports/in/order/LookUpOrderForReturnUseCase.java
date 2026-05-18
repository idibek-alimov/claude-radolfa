package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.Order;

public interface LookUpOrderForReturnUseCase {
    Order execute(Long orderId, Long staffUserId);
}
