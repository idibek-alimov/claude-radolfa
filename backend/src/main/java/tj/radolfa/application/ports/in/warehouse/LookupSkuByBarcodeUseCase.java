package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.domain.model.PlacementView;
import tj.radolfa.domain.model.Sku;

import java.util.List;

public interface LookupSkuByBarcodeUseCase {

    record Result(Sku sku, String productName, List<PlacementView> placements) {}

    Result execute(String barcode);
}
