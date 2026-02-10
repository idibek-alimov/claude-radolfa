package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.DeleteProductUseCase;
import tj.radolfa.application.ports.out.DeleteProductPort;

@Service
public class DeleteProductService implements DeleteProductUseCase {

    private final DeleteProductPort deleteProductPort;

    public DeleteProductService(DeleteProductPort deleteProductPort) {
        this.deleteProductPort = deleteProductPort;
    }

    @Override
    @Transactional
    public void execute(String erpId) {
        deleteProductPort.deleteByErpId(erpId);
    }
}
