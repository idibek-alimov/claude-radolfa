package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.SaveDiscountChangePort;
import tj.radolfa.domain.model.DiscountChange;
import tj.radolfa.infrastructure.persistence.entity.DiscountChangeEntity;
import tj.radolfa.infrastructure.persistence.repository.DiscountChangeJpaRepository;

@Component
public class DiscountChangeJpaAdapter implements SaveDiscountChangePort {

    private final DiscountChangeJpaRepository repository;

    public DiscountChangeJpaAdapter(DiscountChangeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public DiscountChange save(DiscountChange change) {
        var entity = new DiscountChangeEntity(
                null,
                change.discountId(),
                change.changeType(),
                change.oldValueJson(),
                change.newValueJson(),
                change.actorUserId(),
                change.occurredAt());
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    private DiscountChange toDomain(DiscountChangeEntity e) {
        return new DiscountChange(
                e.getId(),
                e.getDiscountId(),
                e.getChangeType(),
                e.getOldValue(),
                e.getNewValue(),
                e.getActorUserId(),
                e.getOccurredAt());
    }
}
