package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.LookupSkuByBarcodeUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuByBarcodePort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Sku;

@Service
public class LookupSkuByBarcodeService implements LookupSkuByBarcodeUseCase {

    private final LoadSkuByBarcodePort   loadSkuByBarcodePort;
    private final LoadListingVariantPort loadListingVariantPort;
    private final LoadProductBasePort    loadProductBasePort;

    public LookupSkuByBarcodeService(LoadSkuByBarcodePort loadSkuByBarcodePort,
                                     LoadListingVariantPort loadListingVariantPort,
                                     LoadProductBasePort loadProductBasePort) {
        this.loadSkuByBarcodePort   = loadSkuByBarcodePort;
        this.loadListingVariantPort = loadListingVariantPort;
        this.loadProductBasePort    = loadProductBasePort;
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

        return new Result(sku, productName);
    }
}
