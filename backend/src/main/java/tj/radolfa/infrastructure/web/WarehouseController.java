package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.adapter.InventoryTransactionJpaAdapter;
import tj.radolfa.infrastructure.web.dto.InventoryTransactionDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/warehouse")
@Tag(name = "Admin — Warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final InventoryTransactionJpaAdapter inventoryTransactionAdapter;

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
}
