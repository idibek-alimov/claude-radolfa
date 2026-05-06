package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.Pickpoint;

import java.util.Optional;

public interface GetAdminOrderDetailUseCase {

    Result execute(Long orderId);

    record Result(Order order,
                  Optional<Pickpoint> pickpoint,
                  String userPhone,
                  String userName) {}
}
