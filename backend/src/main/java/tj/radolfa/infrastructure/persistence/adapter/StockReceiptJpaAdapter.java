package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadStockReceiptPort;
import tj.radolfa.application.ports.out.SaveStockReceiptPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.StockReceipt;
import tj.radolfa.infrastructure.persistence.entity.StockReceiptEntity;
import tj.radolfa.infrastructure.persistence.entity.StockReceiptItemEntity;
import tj.radolfa.infrastructure.persistence.mappers.StockReceiptMapper;
import tj.radolfa.infrastructure.persistence.repository.StockReceiptRepository;

import java.util.List;
import java.util.Optional;

@Component
public class StockReceiptJpaAdapter implements SaveStockReceiptPort, LoadStockReceiptPort {

    private final StockReceiptRepository repository;
    private final StockReceiptMapper     mapper;

    public StockReceiptJpaAdapter(StockReceiptRepository repository, StockReceiptMapper mapper) {
        this.repository = repository;
        this.mapper     = mapper;
    }

    @Override
    public StockReceipt save(StockReceipt receipt) {
        StockReceiptEntity entity = mapper.toEntity(receipt);
        for (StockReceiptItemEntity item : entity.getItems()) {
            item.setReceipt(entity);
        }
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<StockReceipt> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<StockReceipt> findAllPaged(int page, int size, String search) {
        String normalized = (search == null) ? "" : search.trim();
        var pageable = PageRequest.of(page - 1, size);
        Page<StockReceiptEntity> result = repository.findAllWithSearch(normalized, pageable);
        return toPageResult(result, page);
    }

    private PageResult<StockReceipt> toPageResult(Page<StockReceiptEntity> page, int pageNumber) {
        List<StockReceipt> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, page.getTotalElements(), pageNumber, page.getSize(), page.isLast());
    }
}
