package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.LookupSkuByBarcodeUseCase;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuByBarcodePort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.PlacementView;
import tj.radolfa.domain.model.Sku;

import java.util.List;

@Service
public class LookupSkuByBarcodeService implements LookupSkuByBarcodeUseCase {

    private final LoadSkuByBarcodePort   loadSkuByBarcodePort;
    private final LoadListingVariantPort loadListingVariantPort;
    private final LoadProductBasePort    loadProductBasePort;
    private final InventoryPlacementPort placementPort;
    private final LoadWarehousePort      loadWarehousePort;

    public LookupSkuByBarcodeService(LoadSkuByBarcodePort loadSkuByBarcodePort,
                                     LoadListingVariantPort loadListingVariantPort,
                                     LoadProductBasePort loadProductBasePort,
                                     InventoryPlacementPort placementPort,
                                     LoadWarehousePort loadWarehousePort) {
        this.loadSkuByBarcodePort   = loadSkuByBarcodePort;
        this.loadListingVariantPort = loadListingVariantPort;
        this.loadProductBasePort    = loadProductBasePort;
        this.placementPort          = placementPort;
        this.loadWarehousePort      = loadWarehousePort;
    }

    @Override
    @Transactional(readOnly = true)
    public Result execute(String barcode) {
        Sku sku = loadSkuByBarcodePort.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SKU not found by barcode: " + barcode));

        String productName = loadListingVariantPort.findVariantById(sku.getListingVariantId())
                .flatMap(v -> loadProductBasePort.findById(v.getProductBaseId()))
                .map(pb -> pb.getName())
                .orElse(sku.getSkuCode());

        Long wh = loadWarehousePort.findDefault().id();
        List<PlacementView> placements = placementPort.placementViewsForSku(sku.getId(), wh);

        return new Result(sku, productName, placements);
    }
}
