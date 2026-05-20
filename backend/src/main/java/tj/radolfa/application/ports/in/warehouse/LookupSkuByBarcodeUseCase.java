package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.domain.model.Sku;

public interface LookupSkuByBarcodeUseCase {

    record Result(Sku sku, String productName) {}

    Result execute(String barcode);
}
