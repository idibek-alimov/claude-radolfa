package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.in.warehouse.AssignSkuToBinUseCase;
import tj.radolfa.application.ports.in.warehouse.CreateStockReceiptUseCase;
import tj.radolfa.application.ports.in.warehouse.GetStockReceiptByIdUseCase;
import tj.radolfa.application.ports.in.warehouse.GetStockReceiptsUseCase;
import tj.radolfa.application.ports.in.warehouse.LookupSkuByBarcodeUseCase;
import tj.radolfa.application.ports.in.warehouse.ManageWarehouseLocationUseCase;
import tj.radolfa.application.ports.in.warehouse.ReviewCustomerReturnItemsUseCase;
import tj.radolfa.domain.model.WarehouseBin;
import tj.radolfa.domain.model.WarehouseShelf;
import tj.radolfa.domain.model.WarehouseZone;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.StockReceipt;
import tj.radolfa.infrastructure.persistence.adapter.InventoryTransactionJpaAdapter;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.AssignBinRequestDto;
import tj.radolfa.infrastructure.web.dto.CreateBinRequestDto;
import tj.radolfa.infrastructure.web.dto.CreateShelfRequestDto;
import tj.radolfa.infrastructure.web.dto.CreateStockReceiptRequestDto;
import tj.radolfa.infrastructure.web.dto.CreateZoneRequestDto;
import tj.radolfa.infrastructure.web.dto.InventoryTransactionDto;
import tj.radolfa.infrastructure.web.dto.ReviewReturnItemsRequestDto;
import tj.radolfa.infrastructure.web.dto.SkuLookupDto;
import tj.radolfa.infrastructure.web.dto.StockReceiptDto;
import tj.radolfa.infrastructure.web.dto.WarehouseBinDto;
import tj.radolfa.infrastructure.web.dto.WarehouseShelfDto;
import tj.radolfa.infrastructure.web.dto.WarehouseZoneDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/warehouse")
@Tag(name = "Admin — Warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final InventoryTransactionJpaAdapter     inventoryTransactionAdapter;
    private final CreateStockReceiptUseCase          createStockReceiptUseCase;
    private final GetStockReceiptsUseCase            getStockReceiptsUseCase;
    private final GetStockReceiptByIdUseCase         getStockReceiptByIdUseCase;
    private final ReviewCustomerReturnItemsUseCase   reviewCustomerReturnItemsUseCase;
    private final LookupSkuByBarcodeUseCase          lookupSkuByBarcodeUseCase;
    private final ManageWarehouseLocationUseCase     manageWarehouseLocationUseCase;
    private final AssignSkuToBinUseCase              assignSkuToBinUseCase;

    // ── Inventory history ─────────────────────────────────────────────────────

    @GetMapping("/skus/{skuId}/inventory-history")
    @Operation(summary = "Get inventory transaction history for a SKU")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponse<InventoryTransactionDto>> getInventoryHistory(
            @PathVariable Long skuId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageResult<InventoryTransaction> result =
                inventoryTransactionAdapter.findBySkuId(skuId, page, size);
        List<InventoryTransactionDto> dtos = result.content().stream()
                .map(InventoryTransactionDto::from)
                .toList();
        return ResponseEntity.ok(PageResponse.from(
                new PageResult<>(dtos, result.totalElements(), result.number(), result.size(), result.last())));
    }

    // ── Barcode lookup ────────────────────────────────────────────────────────

    @GetMapping("/skus/by-barcode")
    @Operation(summary = "Look up a SKU by its barcode (scanner-driven)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SkuLookupDto> lookupByBarcode(@RequestParam String code) {
        LookupSkuByBarcodeUseCase.Result result = lookupSkuByBarcodeUseCase.execute(code);
        return ResponseEntity.ok(SkuLookupDto.from(result.sku(), result.productName(), result.binLocation()));
    }

    // ── Stock receipts ────────────────────────────────────────────────────────

    @PostMapping("/stock-receipts")
    @Operation(summary = "Create a stock receipt and apply stock increments")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<StockReceiptDto> createReceipt(
            @RequestBody @Valid CreateStockReceiptRequestDto request,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        var command = new CreateStockReceiptUseCase.Command(
                principal.userId(),
                request.supplierReference(),
                request.notes(),
                request.items().stream()
                        .map(i -> new CreateStockReceiptUseCase.ItemCommand(i.skuId(), i.quantity(), i.notes()))
                        .toList());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StockReceiptDto.from(createStockReceiptUseCase.execute(command)));
    }

    @GetMapping("/stock-receipts")
    @Operation(summary = "List stock receipts (paginated, searchable by supplier reference)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponse<StockReceiptDto>> listReceipts(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<StockReceipt> result = getStockReceiptsUseCase.execute(page, size, search);
        List<StockReceiptDto> dtos = result.content().stream().map(StockReceiptDto::from).toList();
        return ResponseEntity.ok(PageResponse.from(
                new PageResult<>(dtos, result.totalElements(), result.number(), result.size(), result.last())));
    }

    @GetMapping("/stock-receipts/{id}")
    @Operation(summary = "Get a stock receipt by ID with all line items")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<StockReceiptDto> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(StockReceiptDto.from(getStockReceiptByIdUseCase.execute(id)));
    }

    // ── Warehouse location CRUD ───────────────────────────────────────────────

    @PostMapping("/zones")
    @Operation(summary = "Create a warehouse zone")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<WarehouseZoneDto> createZone(@RequestBody @Valid CreateZoneRequestDto request) {
        WarehouseZone zone = manageWarehouseLocationUseCase.createZone(request.code(), request.label());
        return ResponseEntity.status(HttpStatus.CREATED).body(WarehouseZoneDto.from(zone));
    }

    @GetMapping("/zones")
    @Operation(summary = "List all warehouse zones")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WarehouseZoneDto>> listZones() {
        return ResponseEntity.ok(manageWarehouseLocationUseCase.listZones()
                .stream().map(WarehouseZoneDto::from).toList());
    }

    @DeleteMapping("/zones/{zoneId}")
    @Operation(summary = "Delete a warehouse zone (cascades to shelves and bins)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteZone(@PathVariable Long zoneId) {
        manageWarehouseLocationUseCase.deleteZone(zoneId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/zones/{zoneId}/shelves")
    @Operation(summary = "Create a shelf within a zone")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<WarehouseShelfDto> createShelf(@PathVariable Long zoneId,
                                                          @RequestBody @Valid CreateShelfRequestDto request) {
        WarehouseShelf shelf = manageWarehouseLocationUseCase.createShelf(zoneId, request.code(), request.label());
        return ResponseEntity.status(HttpStatus.CREATED).body(WarehouseShelfDto.from(shelf));
    }

    @GetMapping("/zones/{zoneId}/shelves")
    @Operation(summary = "List shelves within a zone")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WarehouseShelfDto>> listShelves(@PathVariable Long zoneId) {
        return ResponseEntity.ok(manageWarehouseLocationUseCase.listShelves(zoneId)
                .stream().map(WarehouseShelfDto::from).toList());
    }

    @DeleteMapping("/shelves/{shelfId}")
    @Operation(summary = "Delete a shelf (cascades to bins)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteShelf(@PathVariable Long shelfId) {
        manageWarehouseLocationUseCase.deleteShelf(shelfId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/shelves/{shelfId}/bins")
    @Operation(summary = "Create a bin within a shelf")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<WarehouseBinDto> createBin(@PathVariable Long shelfId,
                                                      @RequestBody @Valid CreateBinRequestDto request) {
        WarehouseBin bin = manageWarehouseLocationUseCase.createBin(shelfId, request.code());
        return ResponseEntity.status(HttpStatus.CREATED).body(WarehouseBinDto.from(bin));
    }

    @GetMapping("/shelves/{shelfId}/bins")
    @Operation(summary = "List bins within a shelf")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WarehouseBinDto>> listBins(@PathVariable Long shelfId) {
        return ResponseEntity.ok(manageWarehouseLocationUseCase.listBins(shelfId)
                .stream().map(WarehouseBinDto::from).toList());
    }

    @DeleteMapping("/bins/{binId}")
    @Operation(summary = "Delete a bin (SKU bin_id set to null)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteBin(@PathVariable Long binId) {
        manageWarehouseLocationUseCase.deleteBin(binId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/skus/{skuId}/bin")
    @Operation(summary = "Assign or unassign a SKU to a warehouse bin")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> assignSkuToBin(@PathVariable Long skuId,
                                               @RequestBody AssignBinRequestDto request) {
        assignSkuToBinUseCase.execute(skuId, request.binId());
        return ResponseEntity.noContent().build();
    }

    // ── Customer return resellability review ──────────────────────────────────

    @PostMapping("/customer-returns/{returnId}/review-items")
    @Operation(summary = "Review resellability of items in a walk-in return")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> reviewReturnItems(
            @PathVariable Long returnId,
            @RequestBody @Valid ReviewReturnItemsRequestDto request,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        var command = new ReviewCustomerReturnItemsUseCase.Command(
                returnId, principal.userId(),
                request.reviews().stream()
                        .map(r -> new ReviewCustomerReturnItemsUseCase.ItemReview(
                                r.orderItemId(), r.resellability()))
                        .toList());
        reviewCustomerReturnItemsUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}
