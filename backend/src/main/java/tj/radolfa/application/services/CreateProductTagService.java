package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.CreateProductTagUseCase;
import tj.radolfa.application.ports.out.LoadProductTagPort;
import tj.radolfa.application.ports.out.SaveProductTagPort;
import tj.radolfa.domain.exception.DuplicateResourceException;

@Service
@Transactional
public class CreateProductTagService implements CreateProductTagUseCase {

    private final LoadProductTagPort loadProductTagPort;
    private final SaveProductTagPort saveProductTagPort;

    public CreateProductTagService(LoadProductTagPort loadProductTagPort,
                                   SaveProductTagPort saveProductTagPort) {
        this.loadProductTagPort = loadProductTagPort;
        this.saveProductTagPort = saveProductTagPort;
    }

    @Override
    public Long execute(String name, String colorHex) {
        if (loadProductTagPort.existsByName(name)) {
            throw new DuplicateResourceException("A tag with the name '" + name + "' already exists.");
        }
        return saveProductTagPort.save(name, colorHex).id();
    }
}
