package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ProductTag;

public interface SaveProductTagPort {
    ProductTag save(String name, String colorHex);
    ProductTag update(Long id, String name, String colorHex);
    void delete(Long id);
}
