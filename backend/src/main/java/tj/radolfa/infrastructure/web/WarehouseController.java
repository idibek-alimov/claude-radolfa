package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.in.warehouse.CreateStockReceiptUseCase;
import tj.radolfa.application.ports.in.warehouse.GetStockReceiptByIdUseCase;
import tj.radolfa.application.ports.in.warehouse.GetStockReceiptsUseCase;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.StockReceipt;
import tj.radolfa.infrastructure.persistence.adapter.InventoryTransactionJpaAdapter;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.CreateStockReceiptRequestDto;
import tj.radolfa.infrastructure.web.dto.InventoryTransactionDto;
import tj.radolfa.infrastructure.web.dto.StockReceiptDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/warehouse")
@Tag(name = "Admin — Warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final InventoryTransactionJpaAdapter inventoryTransactionAdapter;
    private final CreateStockReceiptUseCase      createStockReceiptUseCase;
    private final GetStockReceiptsUseCase        getStockReceiptsUseCase;
    private final GetStockReceiptByIdUseCase     getStockReceiptByIdUseCase;

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
}
