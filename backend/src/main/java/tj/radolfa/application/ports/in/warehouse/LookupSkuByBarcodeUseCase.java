package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.domain.model.Sku;

public interface LookupSkuByBarcodeUseCase {

    record Result(Sku sku, String productName, String binLocation) {}

    Result execute(String barcode);
}
