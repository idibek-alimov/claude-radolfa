package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadSkuPriceHistoryPort;
import tj.radolfa.application.ports.out.SavePriceChangePort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.SkuPriceChange;
import tj.radolfa.infrastructure.persistence.entity.SkuPriceChangeEntity;
import tj.radolfa.infrastructure.persistence.repository.SkuPriceChangeJpaRepository;

import java.util.List;
import java.util.Set;

@Component
public class SkuPriceChangeJpaAdapter implements SavePriceChangePort, LoadSkuPriceHistoryPort {

    private static final Set<String> SORTABLE = Set.of("occurredAt", "newPrice");

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

    @Override
    public PageResult<SkuPriceChange> findBySkuId(Long skuId, String sortBy, String sortDir, int page, int size) {
        String col = SORTABLE.contains(sortBy) ? sortBy : "occurredAt";
        Sort.Direction dir = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(dir, col));
        Page<SkuPriceChangeEntity> result = repository.findBySkuId(skuId, pageRequest);

        List<SkuPriceChange> content = result.getContent().stream()
                .map(this::toDomain)
                .toList();

        return new PageResult<>(content, result.getTotalElements(), page, size, result.isLast());
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
