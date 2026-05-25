package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.LookupSkuByBarcodeUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuByBarcodePort;
import tj.radolfa.application.ports.out.LoadWarehouseLocationPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Sku;

@Service
public class LookupSkuByBarcodeService implements LookupSkuByBarcodeUseCase {

    private final LoadSkuByBarcodePort      loadSkuByBarcodePort;
    private final LoadListingVariantPort    loadListingVariantPort;
    private final LoadProductBasePort       loadProductBasePort;
    private final LoadWarehouseLocationPort loadWarehouseLocationPort;

    public LookupSkuByBarcodeService(LoadSkuByBarcodePort loadSkuByBarcodePort,
                                     LoadListingVariantPort loadListingVariantPort,
                                     LoadProductBasePort loadProductBasePort,
                                     LoadWarehouseLocationPort loadWarehouseLocationPort) {
        this.loadSkuByBarcodePort      = loadSkuByBarcodePort;
        this.loadListingVariantPort    = loadListingVariantPort;
        this.loadProductBasePort       = loadProductBasePort;
        this.loadWarehouseLocationPort = loadWarehouseLocationPort;
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

        String binLocation = null;
        if (sku.getBinId() != null) {
            binLocation = loadWarehouseLocationPort.findBinById(sku.getBinId())
                    .flatMap(bin -> loadWarehouseLocationPort.findShelfById(bin.shelfId())
                            .flatMap(shelf -> loadWarehouseLocationPort.findZoneById(shelf.zoneId())
                                    .map(zone -> zone.code() + " / " + shelf.code() + " / " + bin.code())))
                    .orElse(null);
        }

        return new Result(sku, productName, binLocation);
    }
}
