package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.RejectProductUseCase;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductBase;

@Service
public class RejectProductService implements RejectProductUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RejectProductService.class);

    private final LoadProductBasePort      loadProductBasePort;
    private final SaveProductHierarchyPort savePort;

    public RejectProductService(LoadProductBasePort loadProductBasePort,
                                SaveProductHierarchyPort savePort) {
        this.loadProductBasePort = loadProductBasePort;
        this.savePort            = savePort;
    }

    @Override
    @Transactional
    public void execute(Command cmd) {
        ProductBase pb = loadProductBasePort.findById(cmd.productBaseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + cmd.productBaseId()));
        pb.reject(cmd.rejectionReason());
        savePort.saveBase(pb);
        LOG.info("[LIFECYCLE] productBase={} rejected by user={}", cmd.productBaseId(), cmd.actorUserId());
    }
}
