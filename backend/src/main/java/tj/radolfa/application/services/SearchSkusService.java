package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.SearchSkusUseCase;
import tj.radolfa.application.ports.out.SearchSkusPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.SkuSearchRow;

import java.util.List;

@Service
public class SearchSkusService implements SearchSkusUseCase {

    private final SearchSkusPort searchSkusPort;

    public SearchSkusService(SearchSkusPort searchSkusPort) {
        this.searchSkusPort = searchSkusPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SkuSearchRow> execute(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return new PageResult<>(List.of(), 0, page, size, true);
        }
        return searchSkusPort.search(query.trim(), page, size);
    }
}
