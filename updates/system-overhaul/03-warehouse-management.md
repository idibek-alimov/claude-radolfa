# Warehouse Management — Multiphase Plan

> **Rule:** After completing any phase, mark it `✅ Complete` in the status table below.

## Phase Completion Status

| Phase | Description | Status |
|---|---|---|
| 1 | Inventory Transaction Ledger | ✅ Complete |
| 2 | Stock Receipt Document | ✅ Complete |
| 3 | Return Stock Restoration + Resellability Review | ✅ Complete |
| 4 | Barcode Scanning Endpoint | 🔍 Pending |
| 5 | Warehouse Location System | 🔍 Pending |

---

## Context

The warehouse has no visibility. Stock is a single integer per SKU — you can see "45 units" but not how it got there, who changed it, or why. Two confirmed bugs silently destroy stock accuracy: orders returned to warehouse never restore their units to available stock, and customer walk-in returns go to a `SENT_TO_WAREHOUSE` status with no subsequent reconciliation step. Beyond correctness, the warehouse has no receiving process, no barcode-based lookup, and no physical location system.

This plan builds a complete warehouse management layer in five phases, each one building on the previous:

**W1** — Every stock change (sale, cancellation, return, receipt, manual correction) writes an immutable ledger entry. Stock is still a single column for performance, but every transition is now traceable.

**W2** — When new stock physically arrives, admin creates a Stock Receipt document. The system records exactly what arrived, in what quantity, on which date, and from which delivery reference. Each receipt creates RECEIPT ledger entries.

**W3** — Fixes the two stock-restoration bugs. Returns arriving at the warehouse trigger a resellability review: resellable items restore stock (RETURN_RESTORE ledger entry), defective items do not (WRITE_OFF entry). Neither path is implicit — stock only changes after an explicit admin decision.

**W4** — Every SKU barcode already exists and is unique. A new endpoint allows any warehouse device (USB scanner, phone camera) to look up a SKU by scanning its barcode. Future screens wire into this for scan-to-fill behaviour.

**W5** — The warehouse gets a structured location system: Zone → Shelf → Bin. Each SKU can be assigned to a bin. Location is shown in barcode scan results and on the SKU detail screen.

---

## Current Baseline

- **Stock:** `skus.stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0)`. Atomic decrements via `SkuRepository.decrementStockIfAvailable(id, qty)` (checks `>= qty` before subtracting). Increments via `SkuRepository.incrementStock(id, qty)`. Both are used through `UpdateProductStockService` which implements both `UpdateProductStockUseCase` (in-port) and `StockAdjustmentPort` (out-port).
- **Stock consumers:** `CheckoutService` and `CancelOrderService` both inject `StockAdjustmentPort`. Both will need to pass context (orderId, reason) to the new ledger once Phase 1 is complete.
- **Admin stock endpoint:** `PUT /api/v1/admin/skus/{id}/stock` in `ProductManagementController.java`. Calls `UpdateProductStockUseCase.setAbsolute` or `adjust`. No ledger today.
- **Return bugs:** `ConfirmReturnedToWarehouseService.java` transitions order to `RETURNED_TO_WAREHOUSE` but does NOT call `stockAdjustmentPort.increment`. `ConfirmCustomerReturnSentService.java` transitions to `SENT_TO_WAREHOUSE` but semantically the stock restoration belongs at the warehouse-received step — which does not exist yet.
- **Barcode:** `skus.barcode VARCHAR(128) UNIQUE`. `Sku` domain has `String barcode`. `SkuRepository` has no `findByBarcode` method. Barcode lookup is generated but never used in any query path.
- **Location:** No zone, shelf, or bin concept exists anywhere in the domain or schema. `Sku` domain and `SkuEntity` have no location field.
- **Existing patterns to reuse:** `CustomerReturn` + `CustomerReturnItem` (document-with-line-items pattern), `CustomerReturnJpaAdapter` (`toPageResult(Page, pageNumber)` helper, native search query, paged derivation queries), `PageResult<T>` at `domain/model/PageResult.java`, `PageResponse<T>` at `infrastructure/web/PageResponse.java`.
- **No `WarehouseController.java` exists.** All controller files are in `infrastructure/web/`. The new controller belongs there with `@RequestMapping("/api/v1/admin/warehouse")`.
- **Flyway:** Production migrations V1–V17 (V8/V9 absent). V18/V19/V20 claimed by File 1 (Security). V21 claimed by File 2 (Order Lifecycle). This file claims **V22, V23, V24, V25**. Dev seeds `migration-dev/V18__dev_seed.sql` + `migration-dev/V19__dev_reviews.sql` must be renumbered to **V26** + **V27** after all of Files 1, 2, and 3 production migrations are applied.

---

## Constraints & Principles

- Hexagonal architecture. Domain entities in `domain/model/`, ports in `application/ports/{in,out}/`, services in `application/services/`. JPA adapters in `infrastructure/persistence/adapter/`. No domain object holds Spring/JPA imports.
- MapStruct for all DTO ↔ Domain ↔ JPA Entity mapping. No manual `.set()` chains.
- `@Transactional` on application services only.
- No Mockito — hand-written in-memory fakes for all test dependencies.
- No ALTER TABLE in dev — edit the original `CREATE TABLE` in the relevant `VN__.sql` migration directly. For prod a new versioned migration is correct.
- All list endpoints: `page`, `size`, `search` (and `sort` where applicable) as query params, applied server-side. Whitelist sort fields to prevent SQL injection.
- `RecordInventoryTransactionPort` must be called as a side-effect inside the transaction that modifies `stock_quantity` — not in a separate transaction. Both the stock change and the ledger entry must either commit together or roll back together.
- **Ledger entries are immutable.** No UPDATE or DELETE on `inventory_transactions`. Corrections are a new row of type `MANUAL_ADJUSTMENT`.
- Run `./mvnw test -pl backend` after each phase.

---

## Phase 1 — Inventory Transaction Ledger

**Goal:** Every path that changes `stock_quantity` also writes an immutable row to `inventory_transactions`, creating a complete, queryable audit trail of all stock movements.

### 1a — Domain: InventoryTransactionType Enum

**`backend/src/main/java/tj/radolfa/domain/model/InventoryTransactionType.java`** (New File)
```java
public enum InventoryTransactionType {
    SALE,               // stock decremented at checkout
    CANCELLATION,       // stock restored on order cancel or expiry
    RECALL_RETURN,      // stock restored when a recalled order is physically returned
    RETURN_RESTORE,     // stock restored after warehouse resellability review (W3)
    WRITE_OFF,          // item deemed defective after review — paper trail only, no stock change
    RECEIPT,            // stock added via a Stock Receipt document (W2)
    MANUAL_ADJUSTMENT   // admin sets absolute value or positive/negative delta via PUT /skus/{id}/stock
}
```

### 1b — Domain: InventoryTransaction Record

**`backend/src/main/java/tj/radolfa/domain/model/InventoryTransaction.java`** (New File)
```java
public record InventoryTransaction(
    Long id,
    Long skuId,
    int delta,                    // positive = stock added, negative = stock removed, 0 = write-off
    InventoryTransactionType type,
    String referenceType,         // "ORDER", "CUSTOMER_RETURN", "STOCK_RECEIPT", "MANUAL", etc.
    Long referenceId,             // orderId, returnId, receiptId, or null for manual
    Long actorUserId,             // who triggered the change (admin, system job, etc.)
    String notes,                 // optional free text
    Instant occurredAt
) {}
```

### 1c — Flyway Migration: inventory_transactions Table

**`backend/src/main/resources/db/migration/V22__inventory_transactions.sql`** (New File)
```sql
CREATE TABLE inventory_transactions (
    id             BIGSERIAL    PRIMARY KEY,
    sku_id         BIGINT       NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    delta          INTEGER      NOT NULL,
    type           VARCHAR(30)  NOT NULL,
    reference_type VARCHAR(30),
    reference_id   BIGINT,
    actor_user_id  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    notes          TEXT,
    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inv_tx_sku_id      ON inventory_transactions(sku_id);
CREATE INDEX idx_inv_tx_type        ON inventory_transactions(type);
CREATE INDEX idx_inv_tx_occurred_at ON inventory_transactions(occurred_at DESC);
CREATE INDEX idx_inv_tx_reference   ON inventory_transactions(reference_type, reference_id);

-- Backfill: create one RECEIPT row per SKU equal to current stock_quantity.
-- Represents the "opening balance" snapshot so the ledger starts consistent.
INSERT INTO inventory_transactions (sku_id, delta, type, reference_type, notes, occurred_at)
SELECT id, stock_quantity, 'RECEIPT', 'INITIAL_BACKFILL',
       'Opening balance at ledger introduction', NOW()
FROM skus
WHERE stock_quantity > 0;
```

> **Dev note:** In local development, if you drop and recreate the database, the backfill INSERT runs automatically as part of the migration. In production, run the migration during a low-traffic window — the INSERT is a table scan on `skus` and should be fast unless the SKU count is very large.

### 1d — Output Port

**`backend/src/main/java/tj/radolfa/application/ports/out/RecordInventoryTransactionPort.java`** (New File)
```java
public interface RecordInventoryTransactionPort {
    void record(InventoryTransaction transaction);
}
```

### 1e — JPA Entity + Repository + Adapter

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/InventoryTransactionEntity.java`** (New File)
- `@Entity @Table(name = "inventory_transactions")`, Lombok `@Data @NoArgsConstructor @AllArgsConstructor`, NO `extends BaseAuditEntity` (transactions are append-only; `createdAt`/`updatedAt` are not needed — just `occurred_at`).
- Columns map 1:1 to the domain record fields. No `@Version` (no optimistic locking needed on immutable rows).

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/repository/InventoryTransactionRepository.java`** (New File)
```java
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEntity, Long> {
    Page<InventoryTransactionEntity> findBySkuIdOrderByOccurredAtDesc(Long skuId, Pageable pageable);
}
```

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/InventoryTransactionJpaAdapter.java`** (New File)
- `@Component`, implements `RecordInventoryTransactionPort`.
- `record(InventoryTransaction tx)`: map domain → entity (manual mapping, 8 fields), call `repository.save(entity)`.
- Also expose `PageResult<InventoryTransaction> findBySkuId(Long skuId, int page, int size)` for the history endpoint (add as an extra method — does not need a formal in-port since it's only called by the controller directly).

### 1f — Modify UpdateProductStockService

**`backend/src/main/java/tj/radolfa/application/services/UpdateProductStockService.java`** (Modify)
- Add constructor dependency: `RecordInventoryTransactionPort recordInventoryTransactionPort`.
- After every successful stock operation, record a transaction. The ledger entry is written within the same `@Transactional` as the stock change.

**`decrement(Long skuId, int quantity)` — after the atomic decrement succeeds:**
```java
recordInventoryTransactionPort.record(new InventoryTransaction(
    null, skuId, -quantity, InventoryTransactionType.SALE,
    "ORDER", null,   // referenceId = null here; callers that know the orderId can use overloaded version below
    null, null, Instant.now()));
```

**New overloaded method `decrement(Long skuId, int quantity, Long orderId, Long actorUserId)`:**
- Same logic but records `referenceType = "ORDER"`, `referenceId = orderId`, `actorUserId`.
- `CheckoutService` calls this overload passing `order.id()` and `userId`.

**`increment(Long skuId, int quantity)` — add same pattern:**
- Records `InventoryTransactionType.CANCELLATION` with `referenceId = null` (backward-compatible).
- New overload `increment(Long skuId, int quantity, InventoryTransactionType type, String refType, Long refId, Long actorUserId)` — used by `CancelOrderService` and the W3 resellability review service.

**`setAbsolute(Long skuId, int quantity)` — after save:**
- Compute `delta = quantity - sku.getStockQuantity()` before the update.
- Records `InventoryTransactionType.MANUAL_ADJUSTMENT`, `referenceType = "MANUAL"`, `delta = computed delta`, actorUserId from the calling context.
- Since `setAbsolute` does not currently accept an `actorUserId`, add it as a parameter to `UpdateProductStockUseCase.setAbsolute(Long skuId, int quantity, Long actorUserId)` and propagate up from `ProductManagementController`.

### 1g — Update StockAdjustmentPort

**`backend/src/main/java/tj/radolfa/application/ports/out/StockAdjustmentPort.java`** (Modify)
- Add overloads with context:
  ```java
  // existing — kept for backward compatibility
  void decrement(Long skuId, int quantity);
  void increment(Long skuId, int quantity);

  // new — with ledger context
  void decrement(Long skuId, int quantity, Long orderId, Long actorUserId);
  void increment(Long skuId, int quantity, InventoryTransactionType type,
                 String referenceType, Long referenceId, Long actorUserId);
  ```

**Update callers:**
- `CheckoutService`: call `stockAdjustmentPort.decrement(item.getSkuId(), item.getQuantity(), order.id(), userId)`.
- `CancelOrderService.cancelInternal()`: call `stockAdjustmentPort.increment(item.getSkuId(), item.getQuantity(), CANCELLATION, "ORDER", order.id(), actorUserId)`.
- `ConfirmRecallReceivedService` (from File 2): call `stockAdjustmentPort.increment(item.getSkuId(), item.getQuantity(), RECALL_RETURN, "ORDER", order.id(), actorUserId)`.

### 1h — Inventory History Endpoint

**`backend/src/main/java/tj/radolfa/infrastructure/web/WarehouseController.java`** (New File — partial, grows in Phases 2–5)
```java
@RestController
@RequestMapping("/api/v1/admin/warehouse")
@Tag(name = "Admin — Warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final InventoryTransactionJpaAdapter inventoryTransactionAdapter;
    // more dependencies added per phase

    @GetMapping("/skus/{skuId}/inventory-history")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponse<InventoryTransactionDto>> getInventoryHistory(
            @PathVariable Long skuId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageResult<InventoryTransaction> result = inventoryTransactionAdapter.findBySkuId(skuId, page, size);
        List<InventoryTransactionDto> dtos = result.content().stream()
            .map(InventoryTransactionDto::from).toList();
        return ResponseEntity.ok(PageResponse.from(result.withContent(dtos)));
    }
}
```

**`backend/src/main/java/tj/radolfa/infrastructure/web/dto/InventoryTransactionDto.java`** (New File)
```java
public record InventoryTransactionDto(
    Long id,
    int delta,
    String type,
    String referenceType,
    Long referenceId,
    Long actorUserId,
    String notes,
    Instant occurredAt
) {
    public static InventoryTransactionDto from(InventoryTransaction tx) { ... }
}
```

### 1i — Tests

**`backend/src/test/java/tj/radolfa/application/services/UpdateProductStockServiceTest.java`** (Modify — add new cases)
- Fake: `FakeRecordInventoryTransactionPort` captures all `record(...)` calls.
- Test: `decrement(skuId, 3, orderId, userId)` → atomic decrement succeeds → one ledger row recorded with `type = SALE`, `delta = -3`, `referenceId = orderId`.
- Test: `increment(skuId, 2, CANCELLATION, "ORDER", orderId, userId)` → increment succeeds → ledger row with `type = CANCELLATION`, `delta = +2`.
- Test: `setAbsolute(skuId, 10, adminId)` on a SKU with current `stockQuantity = 7` → `delta = +3`, `type = MANUAL_ADJUSTMENT` recorded.
- Test: `decrement` fails (insufficient stock) → `InsufficientStockException` thrown → NO ledger row recorded.

### Verification

1. `./mvnw test -pl backend -Dtest=UpdateProductStockServiceTest` — all pass.
2. V22 migration applies cleanly. `inventory_transactions` table exists; backfill rows for all SKUs with `stock_quantity > 0`.
3. Place and pay for an order. `SELECT * FROM inventory_transactions WHERE sku_id = X ORDER BY occurred_at DESC` — one row with `type = 'SALE'`, `delta = -quantity`, `reference_id = orderId`.
4. Cancel the order — one row added with `type = 'CANCELLATION'`, `delta = +quantity`.
5. `GET /api/v1/admin/warehouse/skus/{id}/inventory-history` → returns `PageResponse` with the two rows in descending time order.
6. `./mvnw test -pl backend` — no regressions.

---

## Phase 2 — Stock Receipt Document

**Goal:** When new stock physically arrives at the warehouse, admin creates a Stock Receipt — a structured document recording the delivery reference, the SKUs, and the quantities received. Submitting a receipt atomically increments stock for all listed SKUs and writes one `RECEIPT` ledger entry per SKU line.

### 2a — Domain: StockReceiptStatus Enum

**`backend/src/main/java/tj/radolfa/domain/model/StockReceiptStatus.java`** (New File)
```java
public enum StockReceiptStatus {
    DRAFT,      // started but not yet submitted (optional — omit if receipts are single-step)
    COMPLETED   // submitted; stock increments have been applied
}
```
For this phase, implement as single-step (no DRAFT state needed). Receipts are `COMPLETED` immediately on creation. Include the enum for future extensibility.

### 2b — Domain: StockReceipt + StockReceiptItem

**`backend/src/main/java/tj/radolfa/domain/model/StockReceiptItem.java`** (New File)
```java
public record StockReceiptItem(
    Long id,
    Long receiptId,
    Long skuId,
    String skuCode,        // snapshot at receipt time (in case SKU is later deleted)
    String productName,    // snapshot
    int quantityReceived,
    String notes           // optional, per-line
) {}
```

**`backend/src/main/java/tj/radolfa/domain/model/StockReceipt.java`** (New File)
```java
public class StockReceipt {
    private final Long id;
    private final Long createdByUserId;
    private final Instant createdAt;
    private final String supplierReference;   // delivery note, PO number, or any external ref
    private final String notes;               // optional overall notes
    private StockReceiptStatus status;
    private final List<StockReceiptItem> items;

    // No mutating methods beyond what's needed for status transition
    public void complete() { this.status = StockReceiptStatus.COMPLETED; }
}
```

### 2c — Flyway Migration: Stock Receipt Tables

**`backend/src/main/resources/db/migration/V23__stock_receipts.sql`** (New File)
```sql
CREATE TABLE stock_receipts (
    id                  BIGSERIAL    PRIMARY KEY,
    created_by_user_id  BIGINT       NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    supplier_reference  VARCHAR(200),
    notes               TEXT,
    status              VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE stock_receipt_items (
    id                  BIGSERIAL    PRIMARY KEY,
    receipt_id          BIGINT       NOT NULL REFERENCES stock_receipts(id) ON DELETE CASCADE,
    sku_id              BIGINT       REFERENCES skus(id) ON DELETE SET NULL,
    sku_code            VARCHAR(64)  NOT NULL,
    product_name        VARCHAR(255) NOT NULL,
    quantity_received   INT          NOT NULL CHECK (quantity_received > 0),
    notes               TEXT,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_receipts_created_by ON stock_receipts(created_by_user_id);
CREATE INDEX idx_stock_receipts_created_at ON stock_receipts(created_at DESC);
CREATE INDEX idx_stock_receipt_items_receipt ON stock_receipt_items(receipt_id);
CREATE INDEX idx_stock_receipt_items_sku ON stock_receipt_items(sku_id);
```

### 2d — JPA Entities + MapStruct Mapper

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/StockReceiptEntity.java`** (New File)
- `@Entity @Table(name = "stock_receipts")`, Lombok `@Data @NoArgsConstructor @AllArgsConstructor`, `extends BaseAuditEntity`.
- `@OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER) List<StockReceiptItemEntity> items`.

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/StockReceiptItemEntity.java`** (New File)
- `@Entity @Table(name = "stock_receipt_items")`, Lombok.
- `@ManyToOne(fetch = LAZY) @JoinColumn(name = "receipt_id", nullable = false) StockReceiptEntity receipt`.

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/mappers/StockReceiptMapper.java`** (New File)
- `@Mapper(componentModel = "spring")` mapping `StockReceipt ↔ StockReceiptEntity` and `StockReceiptItem ↔ StockReceiptItemEntity`.
- Ignore `receipt` back-reference on items (set programmatically in adapter, same pattern as `CustomerReturnMapper`).

### 2e — Output Ports

**`backend/src/main/java/tj/radolfa/application/ports/out/SaveStockReceiptPort.java`** (New File)
```java
public interface SaveStockReceiptPort {
    StockReceipt save(StockReceipt receipt);
}
```

**`backend/src/main/java/tj/radolfa/application/ports/out/LoadStockReceiptPort.java`** (New File)
```java
public interface LoadStockReceiptPort {
    Optional<StockReceipt> findById(Long id);
    PageResult<StockReceipt> findAllPaged(int page, int size, String search);
}
```

### 2f — JPA Repository + Adapter

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/repository/StockReceiptRepository.java`** (New File)
```java
@Query(
    value = """
        SELECT sr.* FROM stock_receipts sr
        WHERE :search IS NULL OR :search = ''
           OR LOWER(sr.supplier_reference) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY sr.created_at DESC
        """,
    countQuery = "SELECT COUNT(sr.id) FROM stock_receipts sr ...",
    nativeQuery = true
)
Page<StockReceiptEntity> findAllWithSearch(@Param("search") String search, Pageable pageable);
```

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/StockReceiptJpaAdapter.java`** (New File)
- `@Component`, implements `SaveStockReceiptPort, LoadStockReceiptPort`.
- `save(StockReceipt)`: map domain → entity, wire `item.setReceipt(entity)` for each child (same cascade pattern as `CustomerReturnJpaAdapter`), call `repository.save(entity)`, map back.
- `findById(Long)`: load entity + map to domain.
- `findAllPaged(int, int, String)`: use `PageRequest.of(page-1, size)` + search query, use `toPageResult(Page, pageNumber)` helper.

### 2g — Input Ports + Services

**`backend/src/main/java/tj/radolfa/application/ports/in/warehouse/CreateStockReceiptUseCase.java`** (New File)
```java
public interface CreateStockReceiptUseCase {
    record ItemCommand(Long skuId, int quantity, String notes) {}
    record Command(Long adminUserId, String supplierReference, String notes, List<ItemCommand> items) {}
    StockReceipt execute(Command command);
}
```

**`backend/src/main/java/tj/radolfa/application/services/CreateStockReceiptService.java`** (New File)
- `@Service @Transactional`.
- Dependencies: `SaveStockReceiptPort`, `LoadSkuPort`, `StockAdjustmentPort`.
- Logic:
  1. Validate `command.items()` is not empty.
  2. For each item: load SKU via `loadSkuPort.findSkuById(item.skuId()).orElseThrow(...)`. Resolve `skuCode` and `productName` snapshots.
  3. Build `StockReceipt` with `status = COMPLETED`.
  4. Save receipt via `saveStockReceiptPort.save(receipt)`.
  5. For each item: `stockAdjustmentPort.increment(item.skuId(), item.quantity(), RECEIPT, "STOCK_RECEIPT", receipt.id(), command.adminUserId())`. This calls the new overloaded `increment` from Phase 1, which writes the ledger entry inside the same transaction.
  6. Return saved receipt.

**`backend/src/main/java/tj/radolfa/application/ports/in/warehouse/GetStockReceiptsUseCase.java`** (New File)
```java
public interface GetStockReceiptsUseCase {
    PageResult<StockReceipt> execute(int page, int size, String search);
}
```
Service: trivial — delegates to `loadStockReceiptPort.findAllPaged(...)`.

**`backend/src/main/java/tj/radolfa/application/ports/in/warehouse/GetStockReceiptByIdUseCase.java`** (New File)
```java
public interface GetStockReceiptByIdUseCase {
    StockReceipt execute(Long id);
    // Throws ResourceNotFoundException if not found.
}
```

### 2h — DTOs

**`backend/src/main/java/tj/radolfa/infrastructure/web/dto/StockReceiptDto.java`** (New File)
```java
public record StockReceiptDto(
    Long id,
    Long createdByUserId,
    Instant createdAt,
    String supplierReference,
    String notes,
    String status,
    List<StockReceiptItemDto> items,
    int totalUnitsReceived
) {
    public static StockReceiptDto from(StockReceipt r) { ... }
}
```

**`backend/src/main/java/tj/radolfa/infrastructure/web/dto/CreateStockReceiptRequestDto.java`** (New File)
```java
public record CreateStockReceiptRequestDto(
    String supplierReference,
    @Size(max = 1000) String notes,
    @NotEmpty List<ReceiptItemRequest> items
) {
    public record ReceiptItemRequest(
        @NotNull Long skuId,
        @Min(1) int quantity,
        String notes
    ) {}
}
```

### 2i — WarehouseController: Stock Receipt Endpoints

**`backend/src/main/java/tj/radolfa/infrastructure/web/WarehouseController.java`** (Modify — add to existing class from Phase 1)
```java
@PostMapping("/stock-receipts")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public ResponseEntity<StockReceiptDto> createReceipt(
        @RequestBody @Valid CreateStockReceiptRequestDto request,
        @AuthenticationPrincipal JwtAuthenticatedUser principal) {
    var command = new CreateStockReceiptUseCase.Command(
        principal.userId(), request.supplierReference(), request.notes(),
        request.items().stream()
            .map(i -> new CreateStockReceiptUseCase.ItemCommand(i.skuId(), i.quantity(), i.notes()))
            .toList());
    return ResponseEntity.status(HttpStatus.CREATED).body(StockReceiptDto.from(createStockReceiptUseCase.execute(command)));
}

@GetMapping("/stock-receipts")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public ResponseEntity<PageResponse<StockReceiptDto>> listReceipts(
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
    PageResult<StockReceipt> result = getStockReceiptsUseCase.execute(page, size, search);
    return ResponseEntity.ok(PageResponse.from(result.withContent(
        result.content().stream().map(StockReceiptDto::from).toList())));
}

@GetMapping("/stock-receipts/{id}")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public ResponseEntity<StockReceiptDto> getReceipt(@PathVariable Long id) {
    return ResponseEntity.ok(StockReceiptDto.from(getStockReceiptByIdUseCase.execute(id)));
}
```

### 2j — Tests

**`backend/src/test/java/tj/radolfa/application/services/CreateStockReceiptServiceTest.java`** (New File)
- Fakes: `FakeSaveStockReceiptPort`, `FakeLoadSkuPort`, `FakeStockAdjustmentPort` (captures increment calls).
- Test: 2-item receipt → receipt saved with `COMPLETED` status → `stockAdjustmentPort.increment` called twice with correct skuIds, quantities, type `RECEIPT`, `referenceType = "STOCK_RECEIPT"`.
- Test: SKU not found in one line → `ResourceNotFoundException`; no receipt saved; no stock incremented.
- Test: `items` empty → validation error before any processing.
- Test: `supplierReference` null → allowed (optional field); receipt saved normally.
- Test: after save, ledger entries recorded with correct `receiptId` as `referenceId`.

### Verification

1. `./mvnw test -pl backend -Dtest=CreateStockReceiptServiceTest` — all pass.
2. V23 migration applies cleanly. `stock_receipts` and `stock_receipt_items` tables exist.
3. `POST /api/v1/admin/warehouse/stock-receipts` with 2 SKUs — 201 response with receipt ID.
4. Confirm `inventory_transactions` has 2 new rows with `type = 'RECEIPT'`, `reference_type = 'STOCK_RECEIPT'`, `reference_id = receiptId`.
5. Confirm `skus.stock_quantity` incremented for both SKUs.
6. `GET /api/v1/admin/warehouse/stock-receipts?page=1&size=20` → `PageResponse` lists the receipt.
7. `GET /api/v1/admin/warehouse/stock-receipts/{id}` → returns receipt with all line items.
8. `./mvnw test -pl backend` — no regressions.

---

## Phase 3 — Return Stock Restoration + Resellability Review

**Goal:** Fix both stock-restoration bugs. For order returns (RETURNED_TO_WAREHOUSE), stock is restored immediately since the package was never opened. For customer walk-in returns, a new warehouse-review step lets admin mark each item RESELLABLE (stock restored) or DEFECTIVE (write-off entry, no stock change).

### 3a — Domain: Resellability Enum

**`backend/src/main/java/tj/radolfa/domain/model/Resellability.java`** (New File)
```java
public enum Resellability {
    PENDING_REVIEW,  // default — not yet assessed
    RESELLABLE,      // item is in good condition; stock will be restored
    DEFECTIVE        // item is damaged or unsellable; stock written off
}
```

### 3b — Add Resellability to CustomerReturnItem

`CustomerReturnItem` is currently an immutable record. Adding `resellability` requires it to become a mutable field OR use a wrapper approach. Since the domain model needs to reflect a state change (admin reviewing each item), make `resellability` a mutable field by converting the record components to a class. However, to minimize blast radius, keep it as a record but change the `resellability` field to use a separate update mechanism.

**Simplest approach:** Keep the record, add `resellability` as a new record component:

**`backend/src/main/java/tj/radolfa/domain/model/CustomerReturnItem.java`** (Modify)
```java
public record CustomerReturnItem(
    Long id,
    Long returnId,
    Long orderItemId,
    int quantity,
    ReturnReason reason,
    String notes,
    Resellability resellability   // ← new; defaults to PENDING_REVIEW
) {}
```

All existing construction sites must add `Resellability.PENDING_REVIEW` as the final argument. The mapper and adapter wire this through.

### 3c — Flyway Migration: resellability Column

**`backend/src/main/resources/db/migration/V24__return_item_resellability.sql`** (New File)
```sql
ALTER TABLE customer_return_items
    ADD COLUMN resellability VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW';
```

> **Dev note:** In local development, add `resellability VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW'` directly to the `CREATE TABLE customer_return_items` block in `V17__pickpoint_operations.sql` instead of this ALTER. The V24 migration is for production deployments only.

### 3d — Update CustomerReturnItemEntity + Mapper

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/CustomerReturnItemEntity.java`** (Modify)
- Add:
  ```java
  @Enumerated(EnumType.STRING)
  @Column(name = "resellability", nullable = false, length = 20)
  private Resellability resellability = Resellability.PENDING_REVIEW;
  ```

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/mappers/CustomerReturnMapper.java`** (Modify)
- Add `resellability` to the item mapping (both directions).

### 3e — Fix ConfirmReturnedToWarehouseService (Order Returns)

**`backend/src/main/java/tj/radolfa/application/services/ConfirmReturnedToWarehouseService.java`** (Modify)
- Add constructor dependency: `StockAdjustmentPort stockAdjustmentPort`, `LoadOrderPort loadOrderPort`.
- After transitioning the order to `RETURNED_TO_WAREHOUSE` and saving:
  ```java
  // Restore stock for all items — the package was never opened by the customer
  order.items().forEach(item -> {
      if (item.getSkuId() != null) {
          stockAdjustmentPort.increment(
              item.getSkuId(), item.getQuantity(),
              InventoryTransactionType.RETURN_RESTORE,
              "ORDER", order.id(), staffUserId);
      }
  });
  ```
- This immediately fixes the W3 bug for the order-return path. No review step is needed here — packages returned from pickup points due to non-collection are assumed to be in original condition.

### 3f — New: ReviewCustomerReturnItemsUseCase

**`backend/src/main/java/tj/radolfa/application/ports/in/warehouse/ReviewCustomerReturnItemsUseCase.java`** (New File)
```java
public interface ReviewCustomerReturnItemsUseCase {
    record ItemReview(Long orderItemId, Resellability resellability) {}
    record Command(Long returnId, Long adminUserId, List<ItemReview> reviews) {}
    void execute(Command command);
    // Reviews each item in a CustomerReturn that has status SENT_TO_WAREHOUSE.
    // Resellable items → stock incremented + RETURN_RESTORE ledger entry.
    // Defective items  → WRITE_OFF ledger entry, no stock change.
    // Updates resellability field on each CustomerReturnItem.
}
```

**`backend/src/main/java/tj/radolfa/application/services/ReviewCustomerReturnItemsService.java`** (New File)
- `@Service @Transactional`.
- Dependencies: `LoadCustomerReturnPort`, `SaveCustomerReturnPort`, `LoadOrderPort`, `StockAdjustmentPort`.
- Logic:
  1. Load `CustomerReturn` by `returnId`; throw `ResourceNotFoundException` if absent.
  2. Assert `customerReturn.status() == SENT_TO_WAREHOUSE` — throw `IllegalStateException("Review is only possible for returns in SENT_TO_WAREHOUSE status")` otherwise.
  3. Build a `Map<Long, Resellability>` from `command.reviews()` keyed by `orderItemId`.
  4. Load the associated `Order` via `loadOrderPort.loadById(customerReturn.orderId())`.
  5. Build `Map<Long, OrderItem>` from `order.items()` keyed by `OrderItem.getId()`.
  6. For each `CustomerReturnItem` in the return:
     - Look up `resellability` from the command map.
     - Update `CustomerReturnItem` record to the new `resellability` value (create new record instance).
     - If `RESELLABLE` and the item's `skuId != null`:
       - `stockAdjustmentPort.increment(skuId, item.quantity(), RETURN_RESTORE, "CUSTOMER_RETURN", customerReturn.id(), command.adminUserId())`.
     - If `DEFECTIVE`:
       - `stockAdjustmentPort.increment(skuId, 0, WRITE_OFF, "CUSTOMER_RETURN", customerReturn.id(), command.adminUserId())` — delta 0 means no stock change; the ledger row is the paper trail. (Alternatively, record via `RecordInventoryTransactionPort` directly with `delta = 0`.)
  7. Rebuild `CustomerReturn` with updated items list. Save.

### 3g — ReviewCustomerReturn Endpoint

**`backend/src/main/java/tj/radolfa/infrastructure/web/dto/ReviewReturnItemsRequestDto.java`** (New File)
```java
public record ReviewReturnItemsRequestDto(
    @NotEmpty List<ItemReviewDto> reviews
) {
    public record ItemReviewDto(
        @NotNull Long orderItemId,
        @NotNull Resellability resellability
    ) {}
}
```

**`backend/src/main/java/tj/radolfa/infrastructure/web/WarehouseController.java`** (Modify — add endpoint)
```java
@PostMapping("/customer-returns/{returnId}/review-items")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public ResponseEntity<Void> reviewReturnItems(
        @PathVariable Long returnId,
        @RequestBody @Valid ReviewReturnItemsRequestDto request,
        @AuthenticationPrincipal JwtAuthenticatedUser principal) {
    var command = new ReviewCustomerReturnItemsUseCase.Command(
        returnId, principal.userId(),
        request.reviews().stream()
            .map(r -> new ReviewCustomerReturnItemsUseCase.ItemReview(r.orderItemId(), r.resellability()))
            .toList());
    reviewCustomerReturnItemsUseCase.execute(command);
    return ResponseEntity.noContent().build();
}
```

### 3h — Tests

**`backend/src/test/java/tj/radolfa/application/services/ConfirmReturnedToWarehouseServiceTest.java`** (Modify — add new case)
- Test: after transition to `RETURNED_TO_WAREHOUSE`, `stockAdjustmentPort.increment` called once per order item with `type = RETURN_RESTORE`.
- Test: item with `skuId = null` (SKU deleted) → increment skipped for that item; others proceed.

**`backend/src/test/java/tj/radolfa/application/services/ReviewCustomerReturnItemsServiceTest.java`** (New File)
- Fakes: `FakeLoadCustomerReturnPort`, `FakeSaveCustomerReturnPort`, `FakeLoadOrderPort`, `FakeStockAdjustmentPort`.
- Test: 2-item return, both marked RESELLABLE → `increment` called twice, both `type = RETURN_RESTORE`; items' `resellability` updated to `RESELLABLE` in saved return.
- Test: 2-item return, one RESELLABLE + one DEFECTIVE → `increment` called once (RETURN_RESTORE) + one WRITE_OFF ledger entry with `delta = 0`; no stock change for defective item.
- Test: return not in `SENT_TO_WAREHOUSE` (e.g. `RECEIVED`) → `IllegalStateException`; no changes.
- Test: all items DEFECTIVE → no stock restored; two WRITE_OFF entries.
- Test: item with no matching `orderItemId` in command → the item keeps `PENDING_REVIEW`; no crash.

### Verification

1. `./mvnw test -pl backend -Dtest=ConfirmReturnedToWarehouseServiceTest,ReviewCustomerReturnItemsServiceTest` — all pass.
2. V24 migration applies; `customer_return_items.resellability` column exists with `DEFAULT 'PENDING_REVIEW'`.
3. Move a READY_FOR_PICKUP order to RETURN_INITIATED → RETURNED_TO_WAREHOUSE. Confirm `inventory_transactions` shows `RETURN_RESTORE` entries and `stock_quantity` is restored.
4. Create a customer walk-in return; confirm-sent to warehouse. `POST /api/v1/admin/warehouse/customer-returns/{id}/review-items` with one RESELLABLE and one DEFECTIVE item. Confirm stock restored for resellable only; WRITE_OFF row in ledger for defective.
5. Attempt review on a `RECEIVED` (not `SENT_TO_WAREHOUSE`) return → 409 with clear error.
6. `./mvnw test -pl backend` — no regressions.

---

## Phase 4 — Barcode Scanning Endpoint

**Goal:** Add a `GET /api/v1/admin/warehouse/skus/by-barcode?code=` endpoint so any scanning device can look up a SKU by scanning its barcode label. Returns SKU details including current stock and (once Phase 5 is complete) bin location.

### 4a — New Output Port

**`backend/src/main/java/tj/radolfa/application/ports/out/LoadSkuByBarcodePort.java`** (New File)
```java
public interface LoadSkuByBarcodePort {
    Optional<Sku> findByBarcode(String barcode);
}
```

### 4b — SkuRepository: Add findByBarcode

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/repository/SkuRepository.java`** (Modify)
- Add:
  ```java
  Optional<SkuEntity> findByBarcode(String barcode);
  ```
  Spring Data derives the query automatically. The existing UNIQUE constraint on `skus.barcode` provides the index — no new migration needed.

### 4c — Implement in ProductHierarchyAdapter

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/ProductHierarchyAdapter.java`** (Modify)
- Add `implements LoadSkuByBarcodePort` to the class declaration.
- Implement:
  ```java
  @Override
  public Optional<Sku> findByBarcode(String barcode) {
      return skuRepository.findByBarcode(barcode)
              .map(skuMapper::toDomain);
  }
  ```
  `skuMapper` is already injected (MapStruct `SkuMapper`). If the mapper field name differs, follow the existing pattern in this adapter.

### 4d — Input Port + Service

**`backend/src/main/java/tj/radolfa/application/ports/in/warehouse/LookupSkuByBarcodeUseCase.java`** (New File)
```java
public interface LookupSkuByBarcodeUseCase {
    Sku execute(String barcode);
    // Throws ResourceNotFoundException if no SKU with this barcode exists.
}
```

**`backend/src/main/java/tj/radolfa/application/services/LookupSkuByBarcodeService.java`** (New File)
- `@Service @Transactional(readOnly = true)`.
- Dependencies: `LoadSkuByBarcodePort`.
- Logic:
  ```java
  return loadSkuByBarcodePort.findByBarcode(barcode)
          .orElseThrow(() -> new ResourceNotFoundException("SKU with barcode", barcode));
  ```

### 4e — SkuLookupDto

**`backend/src/main/java/tj/radolfa/infrastructure/web/dto/SkuLookupDto.java`** (New File)
```java
public record SkuLookupDto(
    Long skuId,
    String skuCode,
    String barcode,
    String productName,        // from the parent ListingVariant/ProductBase — loaded in controller
    String sizeLabel,
    int stockQuantity,
    String binLocation         // null until Phase 5 — format: "Zone A / Shelf 3 / Bin 7"
) {}
```
`productName` is not on `Sku` domain — the controller loads it via a separate `LoadListingVariantPort` or `LoadProductBasePort` call using `sku.getListingVariantId()`. Follow the existing pattern in `ProductManagementController` for resolving product names.

### 4f — Endpoint

**`backend/src/main/java/tj/radolfa/infrastructure/web/WarehouseController.java`** (Modify)
```java
@GetMapping("/skus/by-barcode")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public ResponseEntity<SkuLookupDto> lookupByBarcode(@RequestParam String code) {
    Sku sku = lookupSkuByBarcodeUseCase.execute(code);
    // Resolve product name
    String productName = loadListingVariantPort.findById(sku.getListingVariantId())
            .map(lv -> loadProductBasePort.findById(lv.productBaseId())
                    .map(pb -> pb.name()).orElse("Unknown"))
            .orElse("Unknown");
    return ResponseEntity.ok(new SkuLookupDto(
        sku.getId(), sku.getSkuCode(), sku.getBarcode(),
        productName, sku.getSizeLabel(), sku.getStockQuantity(),
        null  // binLocation — populated in Phase 5
    ));
}
```

### 4g — Tests

**`backend/src/test/java/tj/radolfa/application/services/LookupSkuByBarcodeServiceTest.java`** (New File)
- Fake: `FakeLoadSkuByBarcodePort`.
- Test: valid barcode → SKU returned.
- Test: unknown barcode → `ResourceNotFoundException`.
- Test: barcode is blank/null → `ResourceNotFoundException` (port returns empty).

### Verification

1. `./mvnw test -pl backend -Dtest=LookupSkuByBarcodeServiceTest` — all pass.
2. `GET /api/v1/admin/warehouse/skus/by-barcode?code=2001234567894` (a valid barcode from the DB) → 200 with `SkuLookupDto` including current `stockQuantity`.
3. `GET /api/v1/admin/warehouse/skus/by-barcode?code=NOTEXIST` → 404.
4. As COURIER (wrong role) → 403.
5. `./mvnw test -pl backend` — no regressions.

---

## Phase 5 — Warehouse Location System

**Goal:** The warehouse is divided into Zones, Shelves, and Bins. Each SKU can be assigned to a specific bin. Location is shown in the barcode scan result from Phase 4 and in the SKU admin detail screen.

### 5a — Domain Models

**`backend/src/main/java/tj/radolfa/domain/model/WarehouseZone.java`** (New File)
```java
public record WarehouseZone(Long id, String code, String label) {}
// code: short identifier, e.g. "A", "B", "COLD"
// label: human-readable, e.g. "Zone A — Ground Floor"
```

**`backend/src/main/java/tj/radolfa/domain/model/WarehouseShelf.java`** (New File)
```java
public record WarehouseShelf(Long id, Long zoneId, String code, String label) {}
```

**`backend/src/main/java/tj/radolfa/domain/model/WarehouseBin.java`** (New File)
```java
public record WarehouseBin(Long id, Long shelfId, String code) {}
// code: e.g. "1", "2", "TOP", "BOTTOM"
// Full location display: "{zone.code} / {shelf.code} / {bin.code}"
```

### 5b — Flyway Migration: Location Tables + bin_id on SKUs

**`backend/src/main/resources/db/migration/V25__warehouse_locations.sql`** (New File)
```sql
CREATE TABLE warehouse_zones (
    id         BIGSERIAL   PRIMARY KEY,
    code       VARCHAR(20) NOT NULL UNIQUE,
    label      VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE warehouse_shelves (
    id         BIGSERIAL   PRIMARY KEY,
    zone_id    BIGINT      NOT NULL REFERENCES warehouse_zones(id) ON DELETE CASCADE,
    code       VARCHAR(20) NOT NULL,
    label      VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (zone_id, code)
);

CREATE TABLE warehouse_bins (
    id         BIGSERIAL   PRIMARY KEY,
    shelf_id   BIGINT      NOT NULL REFERENCES warehouse_shelves(id) ON DELETE CASCADE,
    code       VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (shelf_id, code)
);

ALTER TABLE skus
    ADD COLUMN bin_id BIGINT REFERENCES warehouse_bins(id) ON DELETE SET NULL;

CREATE INDEX idx_skus_bin_id         ON skus(bin_id);
CREATE INDEX idx_shelves_zone_id     ON warehouse_shelves(zone_id);
CREATE INDEX idx_bins_shelf_id       ON warehouse_bins(shelf_id);
```

> **Dev note:** Instead of the `ALTER TABLE skus`, add `bin_id BIGINT REFERENCES warehouse_bins(id) ON DELETE SET NULL` directly into the `skus` CREATE TABLE block in `V2__product_catalog.sql`. Also add the three CREATE TABLE statements in a new V25 file (three new tables are acceptable as new files in both dev and prod).

### 5c — JPA Entities

**`WarehouseZoneEntity.java`**, **`WarehouseShelfEntity.java`**, **`WarehouseBinEntity.java`** (New Files)
- Simple `@Entity` classes mapping the domain records. Lombok `@Data`. Zone has `@OneToMany(cascade = ALL) List<WarehouseShelfEntity> shelves`. Shelf has `@OneToMany(cascade = ALL) List<WarehouseBinEntity> bins`. EAGER fetch for all.
- `SkuEntity`: add `@ManyToOne(fetch = LAZY) @JoinColumn(name = "bin_id") WarehouseBinEntity bin`.

**SkuMapper + OrderMapper**: add `bin` field mapping. `Sku` domain: add `Long binId` field (nullable). `SkuLookupDto` in Phase 4 will be populated with the resolved bin location string.

### 5d — Output Ports

**`backend/src/main/java/tj/radolfa/application/ports/out/LoadWarehouseLocationPort.java`** (New File)
```java
public interface LoadWarehouseLocationPort {
    List<WarehouseZone> findAllZones();
    List<WarehouseShelf> findByZoneId(Long zoneId);
    List<WarehouseBin> findByShelfId(Long shelfId);
    Optional<WarehouseBin> findBinById(Long binId);
}
```

**`backend/src/main/java/tj/radolfa/application/ports/out/SaveWarehouseLocationPort.java`** (New File)
```java
public interface SaveWarehouseLocationPort {
    WarehouseZone saveZone(WarehouseZone zone);
    WarehouseShelf saveShelf(WarehouseShelf shelf);
    WarehouseBin saveBin(WarehouseBin bin);
    void deleteZone(Long id);
    void deleteShelf(Long id);
    void deleteBin(Long id);
}
```

**`backend/src/main/java/tj/radolfa/application/ports/out/AssignSkuToBinPort.java`** (New File)
```java
public interface AssignSkuToBinPort {
    void assign(Long skuId, Long binId);   // binId = null means "unassign"
}
```

### 5e — JPA Adapter

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/WarehouseLocationJpaAdapter.java`** (New File)
- `@Component`, implements `LoadWarehouseLocationPort`, `SaveWarehouseLocationPort`, `AssignSkuToBinPort`.
- `assign(skuId, binId)`: use `SkuRepository` to find by ID, set `entity.setBin(binEntity)` (load `WarehouseBinEntity` first if not null), then save. Follow the minimal-update pattern.

### 5f — Input Ports + Services (CRUD)

One use case per location entity. They are all trivially thin — just port delegation. For brevity, create one service class `WarehouseLocationService.java` that implements all location CRUD use cases rather than 9 separate service files.

**`backend/src/main/java/tj/radolfa/application/services/WarehouseLocationService.java`** (New File)
- `@Service @Transactional`.
- Implements the use cases for create/list/delete on zones, shelves, and bins. Validates parent exists before creating a child (e.g. zone must exist before shelf is created).

**`backend/src/main/java/tj/radolfa/application/services/AssignSkuToBinService.java`** (New File)
- `@Service @Transactional`.
- Validates SKU exists and bin exists before assigning.

### 5g — WarehouseController: Location Endpoints

**`backend/src/main/java/tj/radolfa/infrastructure/web/WarehouseController.java`** (Modify)
- CRUD endpoints for zones, shelves, bins under `/api/v1/admin/warehouse/zones`, `.../shelves`, `.../bins`.
- `PUT /api/v1/admin/warehouse/skus/{skuId}/bin` with body `{ "binId": 7 }` → assigns SKU to bin. `binId = null` in body → unassigns.
- All `@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")`.

### 5h — Update SkuLookupDto with Bin Location

**`backend/src/main/java/tj/radolfa/infrastructure/web/WarehouseController.java`** (Modify — the barcode endpoint from Phase 4)
- After loading the SKU, if `sku.getBinId() != null`: load the bin, shelf, and zone and format as `"{zone.code} / {shelf.code} / {bin.code}"`. Set `binLocation` on the DTO.

### 5i — Tests

**`backend/src/test/java/tj/radolfa/application/services/WarehouseLocationServiceTest.java`** (New File)
- Test: create zone → saved with correct code + label.
- Test: create shelf with non-existent zone → `ResourceNotFoundException`.
- Test: create bin → saved under the correct shelf.
- Test: delete zone with shelves → cascades to shelves and bins.

**`backend/src/test/java/tj/radolfa/application/services/AssignSkuToBinServiceTest.java`** (New File)
- Test: valid SKU + valid bin → assignment persisted.
- Test: invalid SKU → `ResourceNotFoundException`.
- Test: `binId = null` → SKU unassigned.

### Verification

1. `./mvnw test -pl backend -Dtest=WarehouseLocationServiceTest,AssignSkuToBinServiceTest` — all pass.
2. V25 migration applies cleanly. Three new tables exist. `skus.bin_id` column present.
3. Create zone "A", shelf "3", bin "7". `PUT /api/v1/admin/warehouse/skus/{id}/bin` `{"binId": 7}`.
4. `GET /api/v1/admin/warehouse/skus/by-barcode?code=...` → response now includes `"binLocation": "A / 3 / 7"`.
5. `./mvnw test -pl backend` — full suite green.

---

## Post-Implementation Checklist

- [ ] `./mvnw test -pl backend` — all tests green after all 5 phases.
- [ ] V22, V23, V24, V25 migrations all apply cleanly on a fresh database in sequence.
- [ ] Dev seeds renamed: `migration-dev/V26__dev_seed.sql`, `migration-dev/V27__dev_reviews.sql`. No checksum conflicts.
- [ ] `grep -r "StockAdjustmentPort" backend/src/main` — `CheckoutService` and `CancelOrderService` call the new context-aware overloads; bare `decrement(skuId, qty)` / `increment(skuId, qty)` are no longer called from these services.
- [ ] `grep -r "stockAdjustmentPort" backend/src/main/java/tj/radolfa/application/services/ConfirmReturnedToWarehouseService.java` — confirms field is present and called.
- [ ] Every stock-change path produces an `inventory_transactions` row in the same DB transaction.
- [ ] No `inventory_transactions` row is ever updated or deleted (immutable ledger guarantee).
- [ ] `GET /api/v1/admin/warehouse/skus/{id}/inventory-history` for a SKU with several orders + a receipt + a manual adjustment → all rows appear in descending time order.
- [ ] `ReviewCustomerReturnItemsService` — a DEFECTIVE item creates a WRITE_OFF ledger row with `delta = 0`; `stock_quantity` does NOT change.
- [ ] Barcode lookup returns 404 for any barcode not in the database (not 500).
- [ ] Bin assignment persists across server restarts (confirmed via DB query, not just in-memory state).
- [ ] Manual end-to-end warehouse flow: receive new stock (POST stock-receipt) → scan a barcode (GET by-barcode) → confirm customer return arrived at warehouse → review items (RESELLABLE + DEFECTIVE) → verify ledger shows RECEIPT + RETURN_RESTORE + WRITE_OFF rows in that order.
