package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.ProductTag;

public interface UpdateProductTagUseCase {
    ProductTag execute(Long id, String name, String colorHex);
}
