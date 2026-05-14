package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.ProductTag;

import java.util.List;

public interface ListAllTagsUseCase {
    List<ProductTag> execute();
}
