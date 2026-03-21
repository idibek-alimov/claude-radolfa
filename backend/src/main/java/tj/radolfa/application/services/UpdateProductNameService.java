package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.UpdateProductNameUseCase;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ProductBase;

/**
 * Renames a ProductBase (MANAGER / ADMIN action).
 *
 * <p>When the external importer is still active this name will be overwritten
 * on the next import cycle. Intended for use once Radolfa is the authoritative source.
 */
@Service
public class UpdateProductNameService implements UpdateProductNameUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateProductNameService.class);

    private final LoadProductBasePort      loadProductBasePort;
    private final SaveProductHierarchyPort savePort;

    public UpdateProductNameService(LoadProductBasePort loadProductBasePort,
                                    SaveProductHierarchyPort savePort) {
        this.loadProductBasePort = loadProductBasePort;
        this.savePort            = savePort;
    }

    @Override
    @Transactional
    public void execute(Long productBaseId, String newName) {
        ProductBase base = loadProductBasePort.findById(productBaseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "ProductBase not found: id=" + productBaseId));

        base.applyExternalUpdate(newName, base.getCategory());
        savePort.saveBase(base);

        LOG.info("[UPDATE-NAME] ProductBase id={} renamed to '{}'", productBaseId, newName);
    }
}
