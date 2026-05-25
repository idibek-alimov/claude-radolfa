package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.SkuSearchRow;

public interface SearchSkusPort {

    PageResult<SkuSearchRow> search(String query, int page, int size);
}
