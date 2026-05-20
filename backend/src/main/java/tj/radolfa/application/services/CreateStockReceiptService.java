package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.CreateStockReceiptUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveStockReceiptPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.StockReceipt;
import tj.radolfa.domain.model.StockReceiptItem;
import tj.radolfa.domain.model.StockReceiptStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CreateStockReceiptService implements CreateStockReceiptUseCase {

    private final SaveStockReceiptPort     saveStockReceiptPort;
    private final LoadSkuPort              loadSkuPort;
    private final LoadListingVariantPort   loadListingVariantPort;
    private final LoadProductBasePort      loadProductBasePort;
    private final StockAdjustmentPort      stockAdjustmentPort;

    public CreateStockReceiptService(SaveStockReceiptPort saveStockReceiptPort,
                                     LoadSkuPort loadSkuPort,
                                     LoadListingVariantPort loadListingVariantPort,
                                     LoadProductBasePort loadProductBasePort,
                                     StockAdjustmentPort stockAdjustmentPort) {
        this.saveStockReceiptPort   = saveStockReceiptPort;
        this.loadSkuPort            = loadSkuPort;
        this.loadListingVariantPort = loadListingVariantPort;
        this.loadProductBasePort    = loadProductBasePort;
        this.stockAdjustmentPort    = stockAdjustmentPort;
    }

    @Override
    @Transactional
    public StockReceipt execute(Command command) {
        if (command.items().isEmpty()) {
            throw new IllegalArgumentException("Stock receipt must contain at least one item");
        }

        Set<Long> skuIds = command.items().stream()
                .map(ItemCommand::skuId).collect(Collectors.toSet());
        Map<Long, Sku> skuMap = loadSkuPort.findAllByIdsAsMap(skuIds);

        for (Long skuId : skuIds) {
            if (!skuMap.containsKey(skuId)) {
                throw new ResourceNotFoundException("SKU not found: " + skuId);
            }
        }

        Set<Long> variantIds = skuMap.values().stream()
                .map(Sku::getListingVariantId).collect(Collectors.toSet());
        Map<Long, ListingVariant> variantMap = loadListingVariantPort.findVariantsByIds(variantIds);

        Set<Long> productBaseIds = variantMap.values().stream()
                .map(ListingVariant::getProductBaseId).collect(Collectors.toSet());
        Map<Long, ProductBase> productBaseMap = loadProductBasePort.findProductsByIds(productBaseIds);

        List<StockReceiptItem> items = command.items().stream()
                .map(cmd -> {
                    Sku sku = skuMap.get(cmd.skuId());
                    ListingVariant variant = variantMap.get(sku.getListingVariantId());
                    String productName = sku.getSkuCode();
                    if (variant != null) {
                        ProductBase base = productBaseMap.get(variant.getProductBaseId());
                        if (base != null) productName = base.getName();
                    }
                    return new StockReceiptItem(null, null, cmd.skuId(),
                            sku.getSkuCode(), productName, cmd.quantity(), cmd.notes());
                })
                .toList();

        StockReceipt receipt = new StockReceipt(null, command.adminUserId(), Instant.now(),
                command.supplierReference(), command.notes(), StockReceiptStatus.COMPLETED, items);

        StockReceipt saved = saveStockReceiptPort.save(receipt);

        for (StockReceiptItem item : saved.getItems()) {
            stockAdjustmentPort.increment(item.skuId(), item.quantityReceived(),
                    InventoryTransactionType.RECEIPT, "STOCK_RECEIPT", saved.getId(),
                    command.adminUserId());
        }

        return saved;
    }
}
