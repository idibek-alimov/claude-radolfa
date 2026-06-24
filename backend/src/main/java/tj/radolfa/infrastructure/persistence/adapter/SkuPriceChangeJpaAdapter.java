package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.SavePriceChangePort;
import tj.radolfa.domain.model.SkuPriceChange;
import tj.radolfa.infrastructure.persistence.entity.SkuPriceChangeEntity;
import tj.radolfa.infrastructure.persistence.repository.SkuPriceChangeJpaRepository;

@Component
public class SkuPriceChangeJpaAdapter implements SavePriceChangePort {

    private final SkuPriceChangeJpaRepository repository;

    public SkuPriceChangeJpaAdapter(SkuPriceChangeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SkuPriceChange save(SkuPriceChange change) {
        var entity = new SkuPriceChangeEntity(
                null,
                change.skuId(),
                change.skuCode(),
                change.oldPrice(),
                change.newPrice(),
                change.actorUserId(),
                change.source(),
                change.occurredAt());
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    private SkuPriceChange toDomain(SkuPriceChangeEntity e) {
        return new SkuPriceChange(
                e.getId(),
                e.getSkuId(),
                e.getSkuCode(),
                e.getOldPrice(),
                e.getNewPrice(),
                e.getActorUserId(),
                e.getSource(),
                e.getOccurredAt());
    }
}
