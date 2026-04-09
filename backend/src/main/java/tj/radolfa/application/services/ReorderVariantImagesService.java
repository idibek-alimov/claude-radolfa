package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.ReorderVariantImagesUseCase;
import tj.radolfa.application.ports.out.SaveListingVariantPort;

import java.util.List;

/**
 * Persists the new sort order of a variant's images.
 * MANAGER or ADMIN — enforced at the controller level.
 */
@Service
public class ReorderVariantImagesService implements ReorderVariantImagesUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ReorderVariantImagesService.class);

    private final SaveListingVariantPort saveVariantPort;

    public ReorderVariantImagesService(SaveListingVariantPort saveVariantPort) {
        this.saveVariantPort = saveVariantPort;
    }

    @Override
    @Transactional
    public void execute(Long variantId, List<Long> orderedImageIds) {
        saveVariantPort.reorderImages(variantId, orderedImageIds);
        LOG.info("[REORDER-IMAGES] variantId={} count={}", variantId, orderedImageIds.size());
    }
}
