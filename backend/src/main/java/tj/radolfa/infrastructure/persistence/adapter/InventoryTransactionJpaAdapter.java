package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.entity.InventoryTransactionEntity;
import tj.radolfa.infrastructure.persistence.repository.InventoryTransactionRepository;

import java.util.List;

@Component
public class InventoryTransactionJpaAdapter implements RecordInventoryTransactionPort {

    private final InventoryTransactionRepository repository;

    public InventoryTransactionJpaAdapter(InventoryTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(InventoryTransaction tx) {
        var entity = new InventoryTransactionEntity(
                null,
                tx.skuId(),
                tx.delta(),
                tx.type(),
                tx.referenceType(),
                tx.referenceId(),
                tx.actorUserId(),
                tx.notes(),
                tx.occurredAt());
        repository.save(entity);
    }

    public PageResult<InventoryTransaction> findBySkuId(Long skuId, int page, int size) {
        var pageable = PageRequest.of(page - 1, size);
        Page<InventoryTransactionEntity> pg =
                repository.findBySkuIdOrderByOccurredAtDesc(skuId, pageable);
        List<InventoryTransaction> content = pg.getContent().stream()
                .map(this::toDomain)
                .toList();
        return new PageResult<>(content, pg.getTotalElements(),
                pageable.getPageNumber() + 1, pageable.getPageSize(), pg.isLast());
    }

    private InventoryTransaction toDomain(InventoryTransactionEntity e) {
        return new InventoryTransaction(
                e.getId(),
                e.getSkuId(),
                e.getDelta(),
                e.getType(),
                e.getReferenceType(),
                e.getReferenceId(),
                e.getActorUserId(),
                e.getNotes(),
                e.getOccurredAt());
    }
}
