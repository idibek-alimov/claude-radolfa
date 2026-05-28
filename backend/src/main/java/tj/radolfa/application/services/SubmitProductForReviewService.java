package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.SubmitProductForReviewUseCase;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductBase;

@Service
public class SubmitProductForReviewService implements SubmitProductForReviewUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SubmitProductForReviewService.class);

    private final LoadProductBasePort      loadProductBasePort;
    private final SaveProductHierarchyPort savePort;

    public SubmitProductForReviewService(LoadProductBasePort loadProductBasePort,
                                         SaveProductHierarchyPort savePort) {
        this.loadProductBasePort = loadProductBasePort;
        this.savePort            = savePort;
    }

    @Override
    @Transactional
    public void execute(Long productBaseId, Long actorUserId) {
        ProductBase pb = loadProductBasePort.findById(productBaseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productBaseId));
        pb.submitForReview();
        savePort.saveBase(pb);
        LOG.info("[LIFECYCLE] productBase={} submitted for review by user={}", productBaseId, actorUserId);
    }
}
