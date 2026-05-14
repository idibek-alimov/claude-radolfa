package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.DeleteProductTagUseCase;
import tj.radolfa.application.ports.out.LoadProductTagPort;
import tj.radolfa.application.ports.out.SaveProductTagPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.exception.TagInUseException;

@Service
@Transactional
public class DeleteProductTagService implements DeleteProductTagUseCase {

    private final LoadProductTagPort loadProductTagPort;
    private final SaveProductTagPort saveProductTagPort;

    public DeleteProductTagService(LoadProductTagPort loadProductTagPort,
                                   SaveProductTagPort saveProductTagPort) {
        this.loadProductTagPort = loadProductTagPort;
        this.saveProductTagPort = saveProductTagPort;
    }

    @Override
    public void execute(Long id) {
        loadProductTagPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
        long inUse = loadProductTagPort.countVariantsUsingTag(id);
        if (inUse > 0) {
            throw new TagInUseException(id, inUse);
        }
        saveProductTagPort.delete(id);
    }
}
