# Warehouse Frontend — Multiphase Plan

> **Rule:** After completing any phase, mark it `✅ Complete` in the status table below.

## Phase Completion Status

| Phase | Description | Status |
|---|---|---|
| 1 | Backend Foundation: `WAREHOUSE_MANAGER` Role + Returns Queue Endpoint | ✅ Complete |
| 2 | Multi-Warehouse Schema Prep (DB-only, business-simple) | 🔍 Pending |
| 3 | Frontend Shell: `/warehouse` Layout, Routing, i18n, Role Guard | 🔍 Pending |
| 4 | Barcode Lookup + Inline Bin Reassignment + Inventory Ledger | 🔍 Pending |
| 5 | Stock Receipts (List + Create + Full Detail Page) | 🔍 Pending |
| 6 | Warehouse Structure (Zones / Shelves / Bins CRUD) | 🔍 Pending |
| 7 | Returns Resellability Queue | 🔍 Pending |

---

## Context

The warehouse backend was built out fully in `03-warehouse-management.md` — inventory transaction ledger, stock receipts, return stock restoration with resellability review, barcode scanning, and the Zone → Shelf → Bin location system. Every endpoint required for warehouse operations exists. The frontend has none of it.

Today the only warehouse-related code in the frontend is `useConfirmReturnedToWarehouse` (used by the pickpoint dashboard) and a `SENT_TO_WAREHOUSE` status badge on customer return rows. There are no warehouse management screens, no warehouse role, and no warehouse navigation.

This plan builds the warehouse management surface for a dedicated full-time **WAREHOUSE_MANAGER** role. It is **desktop-first** (warehouse workstation + USB barcode scanner) and lives in a **separate `/warehouse` layout**, distinct from the courier and pickpoint mobile-first dashboards.

A secondary objective folded into Phase 2: prepare the database schema for future multi-warehouse support without building any multi-warehouse business logic — the "database-ready, business-simple" hybrid. A `warehouses` table + a single seeded default warehouse + `warehouse_id` foreign keys on `warehouse_zones`, `stock_receipts`, and `inventory_transactions`. All existing services default to the single warehouse. No frontend warehouse switcher; the header is generic. When multi-warehouse becomes a real need, the schema is ready and the stock-per-warehouse refactor (which is the genuinely hard part) can be done without painful FK additions on tables full of rows.

**Problems addressed:**

- **P1** — No dedicated warehouse role. Currently `MANAGER` and `ADMIN` can do warehouse work, but `MANAGER` has too much access (catalog editing, order queue, etc.). A warehouse worker should not have catalog edit rights.
- **P2** — No warehouse UI. Backend endpoints for stock receipts, bin assignment, barcode lookup, and return resellability review are unused.
- **P3** — No way for warehouse staff to look up an item by scanning its barcode. Endpoint exists; no UI calls it.
- **P4** — Stock receipts can only be created via API. No form for logging incoming supplier deliveries.
- **P5** — No way to manage the physical warehouse structure (zones, shelves, bins) from the UI.
- **P6** — Customer returns flagged `SENT_TO_WAREHOUSE` have no queue to review. The `POST /review-items` endpoint exists but no UI lists pending reviews. The **GET endpoint to list them is also missing** and must be added.
- **P7** — Multi-warehouse is not a near-term need, but the schema doesn't anticipate it. Cheap prep now avoids costly migrations later.

---

## Constraints & Principles

- Hexagonal backend, FSD frontend. All conventions in root, backend, and frontend `CLAUDE.md` apply.
- **No client-side filtering** of paginated list responses. Every list screen wires `page`, `size`, `search` server-side.
- All user-visible strings use `useTranslations(...)` with full **en / ru / tj** translations.
- Constructor injection only on backend; `@Transactional` on application services; MapStruct for all mapping.
- Edit original `CREATE TABLE` migrations directly in dev (no `ALTER TABLE` files for existing tables). New tables go in new versioned files where possible.
- `WAREHOUSE_MANAGER` is treated as a sibling of `MANAGER` for warehouse endpoints. Explicit `hasAnyRole('WAREHOUSE_MANAGER', 'MANAGER', 'ADMIN')`.
- Stock-changing paths go through the existing `StockAdjustmentPort` (which writes to `inventory_transactions` automatically). **No new stock-write paths in this plan.**
- "Multi-warehouse" in this plan means **schema-ready only**. Stock remains `skus.stock_quantity` (single column). Per-warehouse stock tracking is explicitly out of scope.

---

## Phase 1 — Backend Foundation

**Goal:** Add `WAREHOUSE_MANAGER` across the stack and the missing returns-queue GET endpoint that Phase 7 depends on.

### 1a — UserRole Enum

**`backend/src/main/java/tj/radolfa/domain/model/UserRole.java`** — add `WAREHOUSE_MANAGER` to the enum.

### 1b — Widen role column + seed role row

**`backend/src/main/resources/db/migration/V1__core_schema.sql`** (modify, per dev policy)

- Add `'WAREHOUSE_MANAGER'` to the `INSERT INTO roles (name) VALUES (...)` statement.
- Widen `users.role VARCHAR(16)` → `VARCHAR(20)` in the `CREATE TABLE users` block (`WAREHOUSE_MANAGER` is 17 chars).

### 1c — Add `WAREHOUSE_MANAGER` to all warehouse controller annotations

**`backend/src/main/java/tj/radolfa/infrastructure/web/WarehouseController.java`** — change every `@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")` to `@PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'MANAGER', 'ADMIN')")`. ~12 endpoints affected (zones, shelves, bins, stock-receipts, by-barcode, inventory-history, customer-returns review).

### 1d — Missing endpoint: Returns Queue GET

The backend currently has only `POST /customer-returns/{id}/review-items`. The Phase 7 queue needs a GET to list `SENT_TO_WAREHOUSE` returns.

**`backend/src/main/java/tj/radolfa/application/ports/in/warehouse/GetWarehouseCustomerReturnsUseCase.java`** (new)

```java
public interface GetWarehouseCustomerReturnsUseCase {
    PageResult<CustomerReturn> execute(int page, int size);
}
```

**`backend/src/main/java/tj/radolfa/application/services/GetWarehouseCustomerReturnsService.java`** (new) — `@Service @Transactional(readOnly = true)`. Delegates to a paginated, status-filtered method on `LoadCustomerReturnPort` — add `PageResult<CustomerReturn> findByStatusPaged(CustomerReturnStatus, int, int)` to the port if it doesn't already exist.

**`WarehouseController.java`** — add:

```java
@GetMapping("/customer-returns")
@PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'MANAGER', 'ADMIN')")
public ResponseEntity<PageResponse<CustomerReturnDto>> listReturnsForReview(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
    PageResult<CustomerReturn> result = getWarehouseCustomerReturnsUseCase.execute(page, size);
    var dtos = result.content().stream().map(r -> {
        Order order = loadOrderPort.loadById(r.getOrderId()).orElseThrow();
        User customer = loadUserPort.loadById(order.userId()).orElse(null);
        return CustomerReturnDto.from(r, order, customer);
    }).toList();
    return ResponseEntity.ok(PageResponse.from(result.withContent(dtos)));
}
```

### 1e — Frontend UserRole + Route Guard

- `frontend/src/entities/user/model/types.ts` — add `WAREHOUSE_MANAGER = "WAREHOUSE_MANAGER"` to `UserRole` enum.
- `ProtectedRoute` (verify location during impl): ensure `requiredRole="WAREHOUSE_MANAGER"` accepts `WAREHOUSE_MANAGER` and `ADMIN`. Confirm `requiredRole="MANAGER"` continues to accept `MANAGER` + `ADMIN` (no regression).

### 1f — Dev Seed: WAREHOUSE_MANAGER Test User

`backend/src/main/resources/db/migration-dev/V26__dev_seed.sql` — insert one user with `role = 'WAREHOUSE_MANAGER'` for manual testing.

### 1g — Tests

`GetWarehouseCustomerReturnsServiceTest` (new): asserts status filtering, pagination, descending sort.

### Verification

1. `./mvnw test -pl backend -Dtest=GetWarehouseCustomerReturnsServiceTest` — pass.
2. `./mvnw compile -pl backend` — clean.
3. Seed user logs in → JWT contains `WAREHOUSE_MANAGER` → 403 on `/api/v1/admin/products`, 200 on `/api/v1/admin/warehouse/*`.
4. `GET /api/v1/admin/warehouse/customer-returns?page=1&size=20` returns SENT_TO_WAREHOUSE returns only.

---

## Phase 2 — Multi-Warehouse Schema Prep

**Goal:** Add a `warehouses` table with a seeded default row and `warehouse_id` FKs to the warehouse hierarchy tables. **No business logic change** — every service writes to the default warehouse. Frontend has no warehouse switcher; header stays generic.

### 2a — Domain: `Warehouse`

**`backend/src/main/java/tj/radolfa/domain/model/Warehouse.java`** (new)

```java
public record Warehouse(
    Long id,
    String code,
    String name,
    boolean isDefault,
    Instant createdAt
) {}
```

### 2b — Migrations (edit-original policy)

`warehouses` must exist before any table FKs it. The lowest existing warehouse-related migration is `V22__inventory_transactions.sql`. Add the `CREATE TABLE warehouses` at the **top of V22**, plus the seed row; the existing `inventory_transactions` block then gets a `warehouse_id` FK column.

**`backend/src/main/resources/db/migration/V22__inventory_transactions.sql`** (modify) — prepend:

```sql
CREATE TABLE warehouses (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    is_default BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_warehouses_one_default ON warehouses(is_default) WHERE is_default = TRUE;

INSERT INTO warehouses (code, name, is_default)
VALUES ('MAIN', 'Main Warehouse', TRUE);
```

Then add to the `CREATE TABLE inventory_transactions (...)` block:
`warehouse_id BIGINT NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT DEFAULT 1`

**`backend/src/main/resources/db/migration/V23__stock_receipts.sql`** (modify) — add `warehouse_id BIGINT NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT DEFAULT 1` to the `stock_receipts` CREATE TABLE block.

**`backend/src/main/resources/db/migration/V25__warehouse_locations.sql`** (modify) — add `warehouse_id BIGINT NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT DEFAULT 1` to the `warehouse_zones` CREATE TABLE block. Shelves and bins inherit warehouse scope transitively through their zone.

### 2c — Domain model additions

Add `Long warehouseId` to:
- `WarehouseZone` (record component)
- `StockReceipt` (final field + constructor)
- `InventoryTransaction` (record component)

All construction sites get the default warehouse ID injected.

### 2d — `LoadWarehousePort` + JPA adapter

**`backend/src/main/java/tj/radolfa/application/ports/out/LoadWarehousePort.java`** (new)

```java
public interface LoadWarehousePort {
    Warehouse findDefault();
    Optional<Warehouse> findById(Long id);
}
```

`WarehouseJpaAdapter` (new): `findDefault()` queries via `WarehouseRepository.findByIsDefaultTrue()`. The unique partial index guarantees exactly one row.

### 2e — Wire default warehouse into existing services

Services that create rows in the affected tables inject `LoadWarehousePort` and call `loadWarehousePort.findDefault().id()`:

- `CreateStockReceiptService` — pass `warehouseId` when building the `StockReceipt`.
- `WarehouseLocationService.createZone(...)` — pass `warehouseId` when building `WarehouseZone`.
- `UpdateProductStockService` — pass `warehouseId` when constructing the `InventoryTransaction`.

### 2f — Entities + mappers

New: `WarehouseEntity`. Updated: `WarehouseZoneEntity`, `StockReceiptEntity`, `InventoryTransactionEntity` (add `warehouseId` field). MapStruct mappings updated both directions.

### 2g — Tests

- `CreateStockReceiptServiceTest` (modify): assert saved receipt's `warehouseId` matches default.
- `WarehouseLocationServiceTest` (modify): same for created zones.

### Verification

1. `./mvnw test -pl backend` — green.
2. `SELECT * FROM warehouses;` → one row, `is_default = TRUE`, code `MAIN`.
3. Create a stock receipt via API → `SELECT warehouse_id FROM stock_receipts ORDER BY id DESC LIMIT 1;` matches default warehouse ID.
4. Same for new `warehouse_zones` and new `inventory_transactions`.

---

## Phase 3 — Frontend Shell

**Goal:** Build the empty `/warehouse` app section — layout, sidebar, all route scaffolds, i18n namespace, role guard. No business screens yet.

### 3a — Routes scaffolded

- `frontend/src/app/warehouse/layout.tsx` — wraps children in `WarehouseShell` + `ProtectedRoute requiredRole="WAREHOUSE_MANAGER"`.
- `frontend/src/app/warehouse/page.tsx` — redirects to `/warehouse/lookup`.
- `frontend/src/app/warehouse/lookup/page.tsx` — placeholder.
- `frontend/src/app/warehouse/receipts/page.tsx` — placeholder.
- `frontend/src/app/warehouse/receipts/new/page.tsx` — placeholder.
- `frontend/src/app/warehouse/receipts/[id]/page.tsx` — placeholder.
- `frontend/src/app/warehouse/structure/page.tsx` — placeholder.
- `frontend/src/app/warehouse/returns/page.tsx` — placeholder.

### 3b — Shell widget

**`frontend/src/widgets/warehouse-shell/ui/WarehouseShell.tsx`** (new)

Desktop two-column layout mirroring `AdminShell`:
- Fixed `w-60` sidebar (`bg-slate-900`). Brand header "Warehouse" (generic — no warehouse name). Four nav links: Lookup / Stock Receipts / Structure / Returns Queue. Active route: `bg-primary text-primary-foreground`.
- `flex-1 overflow-y-auto p-6` main area.

### 3c — Role guard

`ProtectedRoute requiredRole="WAREHOUSE_MANAGER"` in `layout.tsx`. Accepts WAREHOUSE_MANAGER + ADMIN per Phase 1e.

### 3d — i18n: `warehouse` namespace

**All three locale files** — add top-level `"warehouse"` namespace with nav + common keys. Per-screen keys added in their respective phases.

```json
"warehouse": {
  "title": "Warehouse",
  "nav": {
    "lookup": "Lookup",
    "receipts": "Stock Receipts",
    "structure": "Structure",
    "returns": "Returns Queue"
  },
  "common": {
    "scan": "Scan barcode",
    "loading": "Loading…",
    "empty": "No results.",
    "search": "Search",
    "save": "Save",
    "cancel": "Cancel",
    "delete": "Delete",
    "confirm": "Confirm",
    "back": "Back",
    "submit": "Submit"
  }
}
```

Russian and Tajik translations provided.

### Verification

1. Log in as WAREHOUSE_MANAGER → `/warehouse` redirects to `/warehouse/lookup`, sidebar shows 4 links.
2. Click each nav link → placeholder content, URL updates.
3. Regular USER → redirected away from all `/warehouse/*` routes.
4. `npx tsc --noEmit` clean. `npm run lint` clean.

---

## Phase 4 — Barcode Lookup + Bin Reassignment + Inventory Ledger

**Goal:** The daily-driver screen. Two-column layout: scan input left, result card right. Click-to-expand bin reassignment (cascading dropdowns). Unassign option. "View History" opens an inventory ledger drawer.

### 4a — Page layout

**`frontend/src/features/warehouse/ui/BarcodeLookupPage.tsx`** (new)

CSS `grid-cols-3`:
- Left (`col-span-1`): scan input panel. Auto-focused large `<Input>`, barcode icon, helper text "Scan or type and press Enter".
- Right (`col-span-2`): result panel. Empty state ("Scan a barcode to begin") until first scan, then renders `SkuResultCard`.

### 4b — Scan input behaviour

- `autoFocus` on mount.
- Submits on **Enter** (`form onSubmit`) — not on keystroke. Physical scanners type fast + Enter; manual typing must also press Enter.
- After a successful scan: input value cleared, input re-focused via `useRef`.
- 404 → `toast.error(t("lookup.notFound"))`, input cleared and re-focused.

### 4c — API hook

**`frontend/src/features/warehouse/api.ts`** (new)

```ts
export function useLookupSkuByBarcode() {
  return useMutation({
    mutationFn: (code: string) =>
      apiClient
        .get<SkuLookupResponse>("/api/v1/admin/warehouse/skus/by-barcode", { params: { code } })
        .then((r) => r.data),
  });
}
```

Mutation (not query) so each scan is explicit. `SkuLookupResponse` mirrors `SkuLookupDto`: `skuId, skuCode, barcode, productName, sizeLabel, stockQuantity, binLocation`.

### 4d — Result card

**`frontend/src/features/warehouse/ui/SkuResultCard.tsx`** (new)

Displays product name (large), size + SKU code, stock quantity (`text-2xl font-bold tabular-nums`), current bin location (pre-formatted string or "Unassigned"). Action row: **Reassign Bin**, **Unassign**, **View History**.

### 4e — Bin reassignment form (click-to-expand, auto-collapse)

**`frontend/src/features/warehouse/ui/BinReassignmentForm.tsx`** (new)

Hidden by default, toggled by "Reassign Bin". Three cascading selects:
- Zone → `GET /warehouse/zones` (loaded eagerly, small list).
- Shelf → `GET /warehouse/zones/{zoneId}/shelves` (loads when zone selected; disabled otherwise).
- Bin → `GET /warehouse/shelves/{shelfId}/bins` (loads when shelf selected; disabled otherwise).

Save disabled until all three selected. On save: `PUT /warehouse/skus/{skuId}/bin` with `{ binId }`. Success → toast, form auto-collapses, result card updates bin location.

### 4f — Unassign

`<AlertDialog>` ("Remove bin assignment?"). On confirm: same endpoint with `{ binId: null }`. Toast + card updates.

### 4g — Inventory History drawer

**`frontend/src/features/warehouse/ui/InventoryHistoryDrawer.tsx`** (new)

Shadcn `<Sheet>` slide-over, right side, `max-w-3xl`. Triggered by "View History".

- Header: product name + SKU code.
- Paginated list of `InventoryTransactionDto` rows. Per row: formatted timestamp, type badge (colour-coded per `InventoryTransactionType`), delta (`+3` / `−2` / `0`), reference link (STOCK_RECEIPT → `/warehouse/receipts/{id}`, ORDER → admin order detail in new tab), actor.
- Pagination block mirrors `CustomerReturnsQueuePage.tsx:180-203`.

Fetches `GET /warehouse/skus/{skuId}/inventory-history?page=&size=20`. `keepPreviousData`.

### 4h — i18n additions

Under `"warehouse"`:
- `lookup.{title, scanPrompt, empty, notFound, stock, size, skuCode, binLocation, unassigned}`
- `lookup.actions.{reassign, unassign, history}`
- `lookup.reassign.{title, zone, shelf, bin, success}`
- `lookup.unassign.{confirmTitle, confirmBody, success}`
- `lookup.history.{title}` + `lookup.history.type.{SALE, CANCELLATION, RECALL_RETURN, RETURN_RESTORE, WRITE_OFF, RECEIPT, MANUAL_ADJUSTMENT}`

All three locales.

### Verification

1. Navigate to `/warehouse/lookup`. Input focused.
2. Scan known barcode → result card appears; input cleared and re-focused.
3. Scan unknown barcode → toast, cleared and re-focused.
4. Click "Reassign Bin" → form expands → cascading dropdowns work → Save → toast, form collapses, bin location updates.
5. Click "Unassign" → confirm → "Unassigned" shown.
6. Click "View History" → drawer with ledger, pagination works.

---

## Phase 5 — Stock Receipts

**Goal:** List, create, and full-page detail for stock receipts. Create page uses the scan-to-add Enter-key pattern. Submit is single-step (immediately `COMPLETED`). Detail is read-only.

### 5a — List page

**`frontend/src/features/warehouse/ui/StockReceiptListPage.tsx`** (new)

- Header: title + "New Receipt" button → `/warehouse/receipts/new`.
- Debounced (300ms) search, `?search=`, resets page to 1 on change.
- Table: Date, Supplier Reference, Total Units, Created By. Click → `/warehouse/receipts/{id}`.
- Pagination block + empty state with CTA.

```ts
export function useStockReceipts(page: number, search: string) {
  return useQuery({
    queryKey: ["stock-receipts", page, search],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<StockReceiptDto>>("/api/v1/admin/warehouse/stock-receipts", {
          params: { page, size: 20, search },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
  });
}
```

### 5b — Create page

**`frontend/src/features/warehouse/ui/StockReceiptCreatePage.tsx`** (new)

1. **Header fields:** Supplier Reference (text input) + Notes (textarea).
2. **Scan-to-add input:** large barcode input, Enter-key trigger. On Enter: calls `useLookupSkuByBarcode`. If SKU already in line-item table → increment quantity. Otherwise → append new row (quantity 1). Input clears and re-focuses. 404 → toast.
3. **Line-item table:** Product Name + Size, editable Quantity (min 1), Notes, Remove button.
4. **Manual entry toggle:** reveals a server-side SKU search dropdown for staff without a scanner.
5. **Submit:** disabled until ≥1 line item. `POST /stock-receipts` → 201 → redirect to `/warehouse/receipts/{newId}`.

### 5c — Detail page

**`frontend/src/features/warehouse/ui/StockReceiptDetailPage.tsx`** (new)

Full read-only page:
- "Back to Receipts" breadcrumb.
- Receipt ID + `COMPLETED` status badge.
- Metadata: Supplier Reference, Date, Created By, Total Units.
- Notes (when present).
- Line-item table: Product Name + Size, SKU Code, Qty Received, Notes.

No edit / delete (backend is immutable on receipts).

### 5d — i18n additions

- `receipts.{title, new, search, empty}`
- `receipts.columns.{date, supplier, units, createdBy}`
- `receipts.create.{title, supplierRef, notes, scanPrompt, manualEntry, submitBtn, submitting, emptyForm, successToast}`
- `receipts.create.lineItem.{product, quantity, notes, remove}`
- `receipts.detail.{title, supplierRef, notes, createdBy, date, totalUnits, status}`

All three locales.

### Verification

1. Empty receipt list → click "New Receipt".
2. Fill supplier ref, scan barcode → row appears.
3. Scan same barcode again → quantity increments to 2 (no duplicate row).
4. Submit → redirected to read-only detail.
5. Back to list → new row present; search by supplier ref works.
6. Barcode Lookup → stock has incremented; "View History" shows a `RECEIPT` row with the receipt ID.

---

## Phase 6 — Warehouse Structure (Zones / Shelves / Bins)

**Goal:** Three-panel CRUD UI for the physical warehouse hierarchy. WAREHOUSE_MANAGER full access. Cascade-delete confirmations name what will be destroyed.

### 6a — Page layout

**`frontend/src/features/warehouse/ui/StructurePage.tsx`** (new)

CSS `grid-cols-12` three-column layout:
- Left (`col-span-3`): Zones — code + label + shelf count. "+" footer. Click = selects.
- Middle (`col-span-4`): Shelves in selected zone. Greyed if no zone selected.
- Right (`col-span-5`): Bins in selected shelf. Greyed if no shelf selected.

Each row: trash icon on hover → delete confirmation.

### 6b — Create dialogs

One shared `<Dialog>` (or three separate) per entity:
- Zone: `code` (max 20) + `label` (max 100).
- Shelf: `code` + `label`. Parent = currently selected zone.
- Bin: `code` only. Parent = currently selected shelf.

Inline validation messages.

### 6c — Delete confirmation

Shared `<AlertDialog>` with cascade messaging:
- Zone: "Deleting Zone 'A' will also delete N shelves and M bins. Any SKUs in those bins will be unassigned."
- Shelf: similar with bin count.
- Bin: "Any SKUs in Bin 'A / 1 / 7' will be unassigned automatically."

Counts from loaded UI state. SKU count note is informational (backend handles the unassign via `ON DELETE SET NULL`).

### 6d — API hooks

```ts
export function useZones() { ... }
export function useShelves(zoneId: number | null) { ... } // enabled: zoneId != null
export function useBins(shelfId: number | null) { ... }
export function useCreateZone() { ... }
export function useCreateShelf() { ... }
export function useCreateBin() { ... }
export function useDeleteZone() { ... }
export function useDeleteShelf() { ... }
export function useDeleteBin() { ... }
```

All mutations invalidate the relevant column's query key.

### 6e — i18n additions

- `structure.{title}`
- `structure.zones.{title, empty}` / `structure.shelves.{title, empty}` / `structure.bins.{title, empty}`
- `structure.create.{zone,shelf,bin}.{title, code, label}`
- `structure.delete.{confirmTitle, confirmBodyZone, confirmBodyShelf, confirmBodyBin}`

All three locales.

### Verification

1. Create Zone "A" → appears.
2. Select Zone A → create Shelf "1" → appears.
3. Select Shelf 1 → create Bins "1", "2", "3".
4. Delete Zone A → confirmation names 1 shelf + 3 bins cascade.
5. Assign a SKU to Bin "1" via Lookup screen. Delete that bin → SKU re-scanned → "Unassigned".

---

## Phase 7 — Returns Resellability Queue

**Goal:** Queue of `SENT_TO_WAREHOUSE` customer returns. WAREHOUSE_MANAGER marks each item RESELLABLE or DEFECTIVE. Backend restores stock for resellable items; writes a `WRITE_OFF` ledger row (delta = 0) for defective.

### 7a — Queue list

**`frontend/src/features/warehouse/ui/ReturnsQueuePage.tsx`** (new)

Powered by `GET /api/v1/admin/warehouse/customer-returns` (Phase 1d).

- Table: Return ID, Order ID, Customer Name, Items count, Sent At.
- Click row → opens `ReturnReviewSheet`.
- Pagination + empty state.

### 7b — Review sheet

**`frontend/src/features/warehouse/ui/ReturnReviewSheet.tsx`** (new)

Shadcn `<Sheet>` slide-over, `max-w-2xl`.

- Header: Return ID, Order ID, customer name, sent-at.
- Item list. Per item: product name + size, quantity, return reason (read-only), notes (read-only).
- Per-item `<ToggleGroup>`: **RESELLABLE** (green) / **DEFECTIVE** (rose). No default — user must choose both.
- Footer: **Submit Review** — disabled until all items have a selection.

On submit: `POST /warehouse/customer-returns/{returnId}/review-items` with `{ reviews: [{ orderItemId, resellability }] }`. Success → toast, sheet closes, queue refetches.

### 7c — API hooks

```ts
export function useWarehouseReturnsQueue(page: number) { ... }

export function useReviewReturnItems() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ returnId, reviews }: { returnId: number; reviews: ItemReview[] }) =>
      apiClient.post(`/api/v1/admin/warehouse/customer-returns/${returnId}/review-items`, { reviews }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["warehouse-returns-queue"] }),
  });
}
```

### 7d — i18n additions

- `returns.{title, empty}`
- `returns.columns.{returnId, orderId, customer, items, sentAt}`
- `returns.review.{title, resellable, defective, submitBtn, submitting, successToast, itemReason, itemNotes, itemQuantity}`

All three locales.

### Verification

1. Create customer return → confirm-sent to warehouse via pickpoint dashboard (existing flow).
2. As WAREHOUSE_MANAGER → `/warehouse/returns` → return appears.
3. Click → sheet opens with items.
4. Mark one RESELLABLE, one DEFECTIVE → Submit → toast, sheet closes, queue refreshes.
5. Resellable SKU: re-scan in Lookup → stock incremented, ledger shows `RETURN_RESTORE`.
6. Defective SKU: stock unchanged, ledger shows `WRITE_OFF` with `delta = 0`.

---

## Post-Implementation Checklist

- [ ] `./mvnw test -pl backend` — full suite green after all 7 phases.
- [ ] V1 (role widening), V22 (warehouses table + FKs), V23, V25 (modified) all apply cleanly on a fresh DB.
- [ ] Default warehouse row exists. Every new `stock_receipt`, `warehouse_zone`, `inventory_transaction` has `warehouse_id` populated.
- [ ] Frontend `npx tsc --noEmit` zero errors. `npm run lint` clean.
- [ ] Seeded WAREHOUSE_MANAGER user can log in and reach `/warehouse/lookup`.
- [ ] Regular USER blocked from all `/warehouse/*` routes.
- [ ] No hardcoded English in any `features/warehouse/**` or `widgets/warehouse-shell/**` — all `useTranslations("warehouse")`.
- [ ] Barcode Lookup input auto-focuses on mount, after scan, and after 404.
- [ ] Bin reassignment dropdowns lazy-load (no preload of all bins on mount).
- [ ] Stock Receipt create: duplicate SKU scans accumulate quantity, do not create duplicate rows.
- [ ] Stock Receipt detail is read-only — no edit or delete actions.
- [ ] Structure cascade-delete confirmation correctly names affected children.
- [ ] Returns Queue uses the `GET /warehouse/customer-returns` endpoint (verify via Network panel).
- [ ] Each phase committed with `Phase N | Warehouse <area>: <change>` message convention.
