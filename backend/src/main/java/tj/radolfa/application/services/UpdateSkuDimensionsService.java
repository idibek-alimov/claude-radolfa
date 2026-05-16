package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.UpdateSkuDimensionsUseCase;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.Sku;

@Service
public class UpdateSkuDimensionsService implements UpdateSkuDimensionsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateSkuDimensionsService.class);

    private final LoadSkuPort              loadSkuPort;
    private final SaveProductHierarchyPort savePort;

    public UpdateSkuDimensionsService(LoadSkuPort loadSkuPort,
                                      SaveProductHierarchyPort savePort) {
        this.loadSkuPort = loadSkuPort;
        this.savePort    = savePort;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        Sku sku = loadSkuPort.findSkuById(command.skuId())
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: id=" + command.skuId()));

        sku.updateLogistics(command.weightKg(), command.lengthCm(), command.widthCm(), command.heightCm());
        savePort.saveSku(sku, sku.getListingVariantId());

        LOG.info("[UPDATE-SKU-DIMS] skuId={} weight={}kg length={}cm width={}cm height={}cm",
                command.skuId(), command.weightKg(), command.lengthCm(),
                command.widthCm(), command.heightCm());
    }
}
