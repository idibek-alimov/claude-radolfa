package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadCustomerReturnStatusChangePort;
import tj.radolfa.application.ports.out.SaveCustomerReturnStatusChangePort;
import tj.radolfa.domain.model.CustomerReturnStatusChange;
import tj.radolfa.infrastructure.persistence.entity.CustomerReturnStatusChangeEntity;
import tj.radolfa.infrastructure.persistence.repository.CustomerReturnStatusChangeJpaRepository;

@Component
public class CustomerReturnStatusChangeJpaAdapter
        implements SaveCustomerReturnStatusChangePort, LoadCustomerReturnStatusChangePort {

    private final CustomerReturnStatusChangeJpaRepository repository;

    public CustomerReturnStatusChangeJpaAdapter(
            CustomerReturnStatusChangeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public CustomerReturnStatusChange append(CustomerReturnStatusChange change) {
        var entity = new CustomerReturnStatusChangeEntity(
                null,
                change.returnId(),
                change.statusFrom(),
                change.statusTo(),
                change.actorUserId(),
                change.reason(),
                change.occurredAt());
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Page<CustomerReturnStatusChange> findByReturnId(Long returnId, Pageable pageable) {
        return repository.findByReturnId(returnId, pageable).map(this::toDomain);
    }

    private CustomerReturnStatusChange toDomain(CustomerReturnStatusChangeEntity e) {
        return new CustomerReturnStatusChange(
                e.getId(),
                e.getReturnId(),
                e.getStatusFrom(),
                e.getStatusTo(),
                e.getActorUserId(),
                e.getReason(),
                e.getOccurredAt());
    }
}
