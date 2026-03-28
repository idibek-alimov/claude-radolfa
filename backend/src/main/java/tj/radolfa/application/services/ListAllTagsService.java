package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.ListAllTagsUseCase;
import tj.radolfa.application.ports.out.LoadProductTagPort;
import tj.radolfa.domain.model.ProductTag;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListAllTagsService implements ListAllTagsUseCase {

    private final LoadProductTagPort loadProductTagPort;

    public ListAllTagsService(LoadProductTagPort loadProductTagPort) {
        this.loadProductTagPort = loadProductTagPort;
    }

    @Override
    public List<ProductTag> execute() {
        return loadProductTagPort.findAll();
    }
}
