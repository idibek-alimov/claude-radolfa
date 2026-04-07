package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UpdateProductTagUseCase;
import tj.radolfa.application.ports.out.LoadProductTagPort;
import tj.radolfa.application.ports.out.SaveProductTagPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductTag;

@Service
@Transactional
public class UpdateProductTagService implements UpdateProductTagUseCase {

    private final LoadProductTagPort loadProductTagPort;
    private final SaveProductTagPort saveProductTagPort;

    public UpdateProductTagService(LoadProductTagPort loadProductTagPort,
                                   SaveProductTagPort saveProductTagPort) {
        this.loadProductTagPort = loadProductTagPort;
        this.saveProductTagPort = saveProductTagPort;
    }

    @Override
    public ProductTag execute(Long id, String name, String colorHex) {
        loadProductTagPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
        if (loadProductTagPort.existsByNameExcludingId(name, id)) {
            throw new DuplicateResourceException("A tag with the name '" + name + "' already exists.");
        }
        return saveProductTagPort.update(id, name, colorHex);
    }
}
