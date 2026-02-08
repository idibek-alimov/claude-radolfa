package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.DeleteProductUseCase;
import tj.radolfa.application.ports.out.DeleteProductPort;
import tj.radolfa.application.ports.out.ElasticsearchProductIndexer;

@Service
public class DeleteProductService implements DeleteProductUseCase {

    private final DeleteProductPort deleteProductPort;
    private final ElasticsearchProductIndexer indexer;

    public DeleteProductService(DeleteProductPort deleteProductPort,
                                ElasticsearchProductIndexer indexer) {
        this.deleteProductPort = deleteProductPort;
        this.indexer = indexer;
    }

    @Override
    @Transactional
    public void execute(String erpId) {
        deleteProductPort.deleteByErpId(erpId);
        indexer.delete(erpId);
    }
}
