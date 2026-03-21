package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.UpdateSkuSizeLabelUseCase;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.Sku;

/**
 * Updates the size label on a specific SKU.
 * MANAGER or ADMIN — enforced at the controller level.
 */
@Service
public class UpdateSkuSizeLabelService implements UpdateSkuSizeLabelUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateSkuSizeLabelService.class);

    private final LoadSkuPort              loadSkuPort;
    private final SaveProductHierarchyPort savePort;

    public UpdateSkuSizeLabelService(LoadSkuPort loadSkuPort,
                                     SaveProductHierarchyPort savePort) {
        this.loadSkuPort = loadSkuPort;
        this.savePort    = savePort;
    }

    @Override
    @Transactional
    public void execute(Long skuId, String newSizeLabel) {
        Sku sku = loadSkuPort.findSkuById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: id=" + skuId));

        sku.updateSizeLabel(newSizeLabel);
        savePort.saveSku(sku, sku.getListingVariantId());

        LOG.info("[UPDATE-SIZE-LABEL] SKU id={} size label updated to '{}'", skuId, newSizeLabel);
    }
}
