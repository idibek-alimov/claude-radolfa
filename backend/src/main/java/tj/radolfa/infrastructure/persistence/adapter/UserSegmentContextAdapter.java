package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadUserSegmentContextPort;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.infrastructure.persistence.repository.OrderRepository;
import tj.radolfa.infrastructure.persistence.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Component
public class UserSegmentContextAdapter implements LoadUserSegmentContextPort {

    private static final List<OrderStatus> EXCLUDED_STATUSES =
            List.of(OrderStatus.PENDING, OrderStatus.CANCELLED);

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public UserSegmentContextAdapter(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public Optional<UserSegmentContext> loadFor(Long userId) {
        return userRepository.findByIdWithTier(userId).map(user -> {
            Long tierId = user.getTier() != null ? user.getTier().getId() : null;
            long confirmedOrderCount = orderRepository.countConfirmedOrdersByUserId(userId, EXCLUDED_STATUSES);
            boolean isNewCustomer = confirmedOrderCount == 0;
            return new UserSegmentContext(userId, tierId, isNewCustomer);
        });
    }
}
