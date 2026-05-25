package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.SkuSearchRow;

public interface SearchSkusUseCase {

    PageResult<SkuSearchRow> execute(String query, int page, int size);
}
