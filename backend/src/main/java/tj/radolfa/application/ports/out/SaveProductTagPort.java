package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ProductTag;

public interface SaveProductTagPort {
    ProductTag save(String name, String colorHex);
}
