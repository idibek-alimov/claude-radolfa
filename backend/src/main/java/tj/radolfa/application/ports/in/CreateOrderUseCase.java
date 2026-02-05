package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Order;
import java.util.Map;

public interface CreateOrderUseCase {
    Order execute(Long userId, Map<String, Integer> items); // Using ERP ID or Internal ID? Let's use ERP ID as it is
                                                            // main identifier exposed
}
