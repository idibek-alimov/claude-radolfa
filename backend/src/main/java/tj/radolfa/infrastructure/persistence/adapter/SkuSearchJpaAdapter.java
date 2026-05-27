package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.SearchSkusPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.SkuSearchRow;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

import java.util.List;

@Component
public class SkuSearchJpaAdapter implements SearchSkusPort {

    private final SkuRepository skuRepository;

    public SkuSearchJpaAdapter(SkuRepository skuRepository) {
        this.skuRepository = skuRepository;
    }

    @Override
    public PageResult<SkuSearchRow> search(String query, int page, int size) {
        var pageable = PageRequest.of(page - 1, size);
        Page<Object[]> pg = skuRepository.searchSkus(query, pageable);
        List<SkuSearchRow> rows = pg.getContent().stream()
                .map(this::toRow)
                .toList();
        return new PageResult<>(rows, pg.getTotalElements(),
                pageable.getPageNumber() + 1, pageable.getPageSize(), pg.isLast());
    }

    private SkuSearchRow toRow(Object[] r) {
        return new SkuSearchRow(
                ((Number) r[0]).longValue(),
                (String) r[1],
                (String) r[2],
                (String) r[3],
                r[4] != null ? ((Number) r[4]).intValue() : 0,
                (String) r[5],
                List.of());
    }
}
