package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.ProductActor;
import tj.radolfa.application.ports.in.product.SubmitProductForReviewUseCase;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.FieldLockException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.UserRole;

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
    public void execute(Long productBaseId, ProductActor actor) {
        ProductBase pb = loadProductBasePort.findById(productBaseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productBaseId));

        assertOwnership(pb, actor);

        pb.submitForReview();
        savePort.saveBase(pb);
        LOG.info("[LIFECYCLE] productBase={} submitted for review by user={}", productBaseId, actor.userId());
    }

    /**
     * Ownership rule for submit-for-review:
     * <ul>
     *   <li>ADMIN / MANAGER → may submit any product.</li>
     *   <li>SELLER → may submit only a product whose {@code seller_id} equals their own seller id.</li>
     * </ul>
     * The check runs before any state transition so a denied submit leaves the database unchanged.
     */
    private void assertOwnership(ProductBase pb, ProductActor actor) {
        if (actor.role() == UserRole.ADMIN || actor.role() == UserRole.MANAGER) {
            return;
        }
        if (actor.role() == UserRole.SELLER) {
            Long ownerSellerId = pb.getSellerId();
            if (ownerSellerId != null && ownerSellerId.equals(actor.sellerId())) {
                return;
            }
            throw new FieldLockException("product",
                    "Seller may only submit their own products for review");
        }
        throw new FieldLockException("product",
                "Role " + actor.role() + " is not permitted to submit products for review");
    }
}
