package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Sku;

import java.util.Optional;

public interface LoadSkuByBarcodePort {

    Optional<Sku> findByBarcode(String barcode);
}
