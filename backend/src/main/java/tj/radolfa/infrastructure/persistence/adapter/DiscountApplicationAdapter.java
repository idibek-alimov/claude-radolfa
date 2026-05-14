package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.SaveDiscountApplicationPort;
import tj.radolfa.domain.model.DiscountApplication;
import tj.radolfa.infrastructure.persistence.entity.DiscountApplicationEntity;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;
import tj.radolfa.infrastructure.persistence.entity.OrderEntity;
import tj.radolfa.infrastructure.persistence.mappers.DiscountApplicationMapper;
import tj.radolfa.infrastructure.persistence.repository.DiscountApplicationRepository;
import tj.radolfa.infrastructure.persistence.repository.DiscountRepository;
import tj.radolfa.infrastructure.persistence.repository.OrderRepository;

@Component
public class DiscountApplicationAdapter implements SaveDiscountApplicationPort {

    private final DiscountApplicationRepository repository;
    private final DiscountApplicationMapper mapper;
    private final DiscountRepository discountRepository;
    private final OrderRepository orderRepository;

    public DiscountApplicationAdapter(DiscountApplicationRepository repository,
                                      DiscountApplicationMapper mapper,
                                      DiscountRepository discountRepository,
                                      OrderRepository orderRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.discountRepository = discountRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public DiscountApplication save(DiscountApplication application) {
        DiscountEntity discountRef = discountRepository.getReferenceById(application.discountId());
        OrderEntity    orderRef    = orderRepository.getReferenceById(application.orderId());
        DiscountApplicationEntity entity = mapper.toEntity(application);
        entity.setDiscount(discountRef);
        entity.setOrder(orderRef);
        return mapper.toDomain(repository.save(entity));
    }
}
