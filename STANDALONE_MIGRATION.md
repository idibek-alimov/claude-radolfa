# Standalone E-Commerce Migration Plan

> **Goal:** Evolve Radolfa from an ERPNext mirror into a fully self-contained e-commerce
> platform — without a big-bang rewrite. Each phase delivers standalone value and leaves
> the app in a working state. All code is development-only; no backwards-compat constraints.

---

## Completion Log
> Fill in this section as phases are marked complete.

| Phase | Status | Summary |
|-------|--------|---------|
| 1 | [x] | Renamed all `erp`-prefixed domain fields, exceptions, methods, DTOs, and infrastructure classes. Moved sync use cases to `ports/in/sync/` subpackage. Renamed `infrastructure/erp/` → `infrastructure/importer/`. Reset all Flyway migrations to a single clean `V1__baseline_clean_schema.sql`. Renamed `SYSTEM` role → `SYNC`. Compile clean, zero `erp` references in executable code. |
| 2 | [ ] | |
| 3 | [ ] | |
| 4 | [ ] | |
| 5 | [ ] | |
| 6 | [ ] | |
| 7 | [ ] | |
| 8 | [ ] | |
| 9 | [ ] | |
| 10 | [ ] | |

---

## Phase 1 — Rename & Restructure (Zero New Functionality)

**Goal:** Purge all `erp`-prefixed names from the codebase. After this phase, no class, field,
method, column, or exception contains the word "erp". The app still functions identically;
only names change. Reset all Flyway migrations to a single clean baseline.

### 1.1 Domain Model Field Renames

| File | Old | New |
|------|-----|-----|
| `ProductBase.java` | `erpTemplateCode` | `externalRef` |
| `ProductBase.java` | `updateFromErp(name, category)` | `applyExternalUpdate(name, category)` |
| `ProductBase.java` | constructor validation message | remove "erp" wording |
| `Sku.java` | `erpItemCode` | `skuCode` |
| `Sku.java` | `updateFromErp(stock, price)` | `updatePriceAndStock(price, stock)` |
| `Sku.java` | getter `getErpItemCode()` | `getSkuCode()` |
| `Order.java` (record) | `erpOrderId` | `externalOrderId` |
| `OrderItem.java` | `erpItemCode` | `skuCode` |
| `UserRole.java` | `SYSTEM` | `SYNC` |

### 1.2 Exception Rename

| File | Action |
|------|--------|
| `domain/exception/ErpLockViolationException.java` | **Delete** |
| `domain/exception/FieldLockException.java` | **Create** — same logic, neutral message: `"Field '{field}' is managed by an authoritative source and cannot be modified directly."` |

Update all references to `ErpLockViolationException` throughout the codebase.

### 1.3 Application Layer Restructure

**Move** all `Sync*UseCase` interfaces into a subpackage `application/ports/in/sync/`:

Files to move (cut-paste, update package declaration):
- `SyncProductHierarchyUseCase.java`
- `SyncUsersUseCase.java`
- `SyncOrdersUseCase.java`
- `SyncLoyaltyPointsUseCase.java`
- `SyncLoyaltyTiersUseCase.java`
- `SyncCategoriesUseCase.java`
- `SyncDiscountUseCase.java`
- `RemoveDiscountUseCase.java`

**Rename** out-port:
- `LogSyncEventPort.java` → `LogImportEventPort.java` (same interface, neutral name)

### 1.4 Infrastructure Web DTOs — Rename ERP-Specific Names

| Old Name | New Name |
|----------|----------|
| `ErpHierarchyPayload.java` | `ImportHierarchyPayload.java` |
| `SyncProductHierarchyPayload.java` (if exists) | `ImportProductPayload.java` |
| `SyncUserPayload.java` | `ImportUserPayload.java` |
| `SyncOrderPayload.java` | `ImportOrderPayload.java` |
| `SyncLoyaltyRequestDto.java` | `ImportLoyaltyPointsDto.java` |
| `SyncLoyaltyTierPayload.java` | `ImportLoyaltyTierDto.java` |
| `SyncDiscountPayload.java` | `ImportDiscountDto.java` |
| `RemoveDiscountPayload.java` | `RemoveImportedDiscountDto.java` |
| `SyncCategoriesPayload.java` | `ImportCategoriesDto.java` |
| `SyncResultDto.java` | `ImportResultDto.java` |

**Rename** infrastructure classes:
- `ErpSyncController.java` → `ImportController.java`
- `ErpSyncLogEntity.java` → `SyncLogEntity.java`
- `ErpSyncLogRepository.java` → `SyncLogRepository.java`
- `ErpSyncLogAdapter.java` → `SyncLogAdapter.java`
- `infrastructure/erp/` package → `infrastructure/importer/`
  - `ErpProductClient.java` → `ProductImportClient.java`
  - `ErpProductClientHttp.java` → `ProductImportClientHttp.java`
  - `ErpProductClientStub.java` → `ProductImportClientStub.java`
  - `ErpProductSnapshot.java` → `ImportedProductSnapshot.java`
  - `batch/ErpSyncJobConfig.java` → `batch/ImportJobConfig.java`
  - `batch/ErpSyncScheduler.java` → `batch/ImportJobScheduler.java`
  - `batch/ErpProductReader.java` → `batch/ImportedProductReader.java`
  - `batch/ErpProductProcessor.java` → `batch/ImportedProductProcessor.java`
  - `batch/ErpProductWriter.java` → `batch/ImportedProductWriter.java`

**Update** `SecurityConfig.java`: all references from `SYSTEM` → `SYNC`.

### 1.5 Database — Full Flyway Reset

**Delete** all existing migration files in `db/migration/` and `db/migration-dev/`:
```
V1__baseline_schema.sql
V2__product_hierarchy.sql
V3__orders.sql
V4__spring_batch_schema.sql
V5__seed_data.sql
V8__categories_and_colors.sql
V10__order_erp_sync.sql
V12__erp_sync_idempotency.sql
V14__loyalty_tiers.sql
V17__rename_price_columns.sql
V19__create_discounts_table.sql
V20__drop_sku_discount_columns.sql
V22__discount_one_to_many.sql
V23__add_variant_attributes.sql
V24__add_product_code.sql
(all dev seed files)
```

**Write** a single `V1__baseline_clean_schema.sql` with:
- Clean column names (no `erp_` prefix anywhere)
- `product_bases.external_ref` (was `erp_template_code`)
- `skus.sku_code` (was `erp_item_code`)
- `orders.external_order_id` (was `erp_order_id`, nullable from the start)
- `order_items.sku_code` (was `erp_item_code`)
- `discounts.external_rule_id` (was `erp_pricing_rule_id`)
- Table `sync_log` (was `erp_sync_log`)
- All tables from all previous migrations consolidated

**Write** `V2__dev_seed.sql` (dev profile only) with realistic seed data.

### 1.6 Update All Remaining References

- All `@PreAuthorize("hasRole('SYSTEM')")` → `@PreAuthorize("hasRole('SYNC')")`
- All `UserRole.SYSTEM` → `UserRole.SYNC`
- Javadoc comments mentioning "ERPNext" in domain classes → reword to "external source"
- Update `CLAUDE.md` `enrichWithErpData()` reference to `applyExternalUpdate()`

**Definition of Done:** `./mvnw test` passes. No `erp` string exists in any `.java` file
(except comments explaining historical context). No `erp_` prefix in any SQL file.

---

## Phase 2 — Domain Enrichment: Cart, Payment, LoyaltyCalculator

**Goal:** Add the core domain models that a standalone e-commerce needs but the ERP-mirror
never required. Pure Java only — no Spring, no JPA, no Jackson.

### 2.1 New Domain Files to Create

**`domain/model/CartStatus.java`** (enum)
```
ACTIVE, CHECKED_OUT, ABANDONED
```

**`domain/model/Cart.java`** (mutable aggregate)
Fields:
- `Long id`
- `Long userId`
- `CartStatus status`
- `List<CartItem> items` (mutable)
- `Instant createdAt`
- `Instant updatedAt`

Methods:
- `addItem(Long skuId, int quantity, Money unitPrice)` — merges if skuId already in cart
- `removeItem(Long skuId)`
- `updateQuantity(Long skuId, int quantity)` — removes if quantity ≤ 0
- `clear()`
- `total()` → `Money` — sum of all items
- `itemCount()` → int
- `checkout()` — transitions status to CHECKED_OUT, throws if not ACTIVE

**`domain/model/CartItem.java`** (mutable entity)
Fields:
- `Long skuId`
- `int quantity`
- `Money unitPriceSnapshot` — price at time of adding to cart

Methods:
- `updateQuantity(int quantity)` — validates > 0
- `lineTotal()` → `Money`

**`domain/model/PaymentStatus.java`** (enum)
```
PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED
```

**`domain/model/Payment.java`** (record — immutable snapshot)
Fields:
- `Long id`
- `Long orderId`
- `Money amount`
- `String currency` (e.g. "TJS")
- `PaymentStatus status`
- `String provider` (e.g. "PAYME", "CLICK", "STUB")
- `String providerTransactionId` (nullable — set after provider confirms)
- `Instant createdAt`
- `Instant completedAt` (nullable)

**`domain/service/LoyaltyCalculator.java`** (pure domain service)
Methods:
- `LoyaltyProfile awardPoints(LoyaltyProfile current, Money orderAmount, List<LoyaltyTier> tiers)`
  — calculates cashback points from order, upgrades tier if threshold crossed
- `Money resolveDiscount(LoyaltyProfile profile, Money orderTotal)`
  — applies tier discount percentage to order total
- `int pointsToRedeem(LoyaltyProfile profile, Money orderTotal)`
  — max redeemable points for a given order (business rule: e.g. max 30% of total)
- `Money pointsToMoney(int points)` — conversion rate (e.g. 1 point = 0.01 TJS)

Write unit tests for `LoyaltyCalculator` immediately.

### 2.2 Definition of Done
All new domain classes compile. `LoyaltyCalculatorTest.java` passes with ≥ 5 test cases
covering tier upgrades, discount application, and points redemption limits.

---

## Phase 3 — Define All Use Case Interfaces & Port Contracts

**Goal:** Declare every contract the standalone platform will need. No implementations yet.
This phase is the architectural blueprint — code compiles but nothing new is wired up.

### 3.1 New In-Ports (`application/ports/in/`)

**Product Management (new `product/` subpackage):**
- `CreateProductUseCase.java` — takes `CreateProductCommand(name, categoryId, colorId, skuDefinitions)`
- `UpdateProductNameUseCase.java` — MANAGER can rename a product
- `UpdateProductPriceUseCase.java` — ADMIN sets price on a SKU
- `UpdateProductStockUseCase.java` — called after order completion or manual adjustment
- `CreateCategoryUseCase.java`
- `DeleteCategoryUseCase.java`

**User Registration:**
- `RegisterUserUseCase.java` — takes phone, auto-assigns USER role, called from OTP verify flow

**Cart:**
- `AddToCartUseCase.java` — takes userId, skuId, quantity
- `RemoveFromCartUseCase.java` — takes userId, skuId
- `UpdateCartItemQuantityUseCase.java` — takes userId, skuId, newQuantity
- `ClearCartUseCase.java` — empty the cart
- `GetCartUseCase.java` — returns cart with current stock validation

**Checkout & Orders:**
- `CheckoutUseCase.java` — takes userId, optional loyaltyPointsToRedeem; returns OrderId
- `CreateOrderUseCase.java` — lower-level: takes explicit items (for admin/import)
- `UpdateOrderStatusUseCase.java` — ADMIN changes order status
- `CancelOrderUseCase.java` — USER or ADMIN cancels

**Payment:**
- `InitiatePaymentUseCase.java` — creates payment record, returns provider redirect URL
- `ConfirmPaymentUseCase.java` — called by webhook, transitions order to PAID
- `RefundPaymentUseCase.java` — ADMIN issues refund

**Loyalty (standalone calculation):**
- `AwardLoyaltyPointsUseCase.java` — called after PAID, uses LoyaltyCalculator
- `RedeemLoyaltyPointsUseCase.java` — called during checkout

### 3.2 New Out-Ports (`application/ports/out/`)

- `LoadCartPort.java` — `findActiveByUserId(Long userId) → Optional<Cart>`
- `SaveCartPort.java` — `save(Cart cart) → Cart`
- `SavePaymentPort.java` — `save(Payment payment) → Payment`
- `LoadPaymentPort.java` — `findByOrderId(Long orderId) → Optional<Payment>`; `findByProviderTransactionId(String id)`
- `PaymentPort.java` — external gateway: `PaymentIntent initiate(Money amount, String orderId, String customerId)`; `RefundResult refund(String transactionId, Money amount)`
- `StockAdjustmentPort.java` — `decrementStock(Long skuId, int quantity)`; `incrementStock(Long skuId, int quantity)`
- `NotificationPort.java` — `sendOrderConfirmation(Long userId, Long orderId)` (stub for now)

### 3.3 Stub Implementations to Create

- `PaymentPortStub.java` (in `infrastructure/payment/stub/`) — always returns success, logs to console
- `NotificationPortStub.java` — logs to console

### 3.4 Definition of Done
All interfaces compile. No implementations required yet. The `@Profile` stubs register in
the Spring context in `dev` and `test` profiles without errors.

---

## Phase 4 — Native Product Management

**Goal:** ADMIN and MANAGER can create and manage the full product catalog through the API
without any external system. The import flow (Phase 1) continues to work in parallel.

### 4.1 Database Migration

**New `V2__make_external_ref_nullable.sql`** (or incorporate into V1 reset from Phase 1):
```sql
ALTER TABLE product_bases ALTER COLUMN external_ref DROP NOT NULL;
ALTER TABLE skus ALTER COLUMN sku_code DROP NOT NULL;
```
(After Phase 1 reset, `external_ref` can be nullable from the start in V1.)

**Auto-generate `sku_code`** if null on save: format `RD-SKU-{padded-id}`.
**Auto-generate `external_ref`** if null on save: format `INTERNAL-{uuid-short}`.

### 4.2 New Services (`application/services/`)

**`CreateProductService.java`** (implements `CreateProductUseCase`)
Logic:
1. Resolve or create `Category` by ID
2. Resolve `Color` by ID
3. Create `ProductBase` with `externalRef = null` (will be generated)
4. For each sku definition in command, create `Sku` with provided price and stock
5. Create `ListingVariant` linking base + color + skus
6. Persist via `SaveProductHierarchyPort`
7. Index via `ListingIndexPort`

**`UpdateProductPriceService.java`** (implements `UpdateProductPriceUseCase`)
- Loads `Sku`, calls `sku.updatePriceAndStock(newPrice, sku.getStockQuantity())`
- ADMIN role required — no role check in service (done at controller level)

**`UpdateProductStockService.java`** (implements `UpdateProductStockUseCase` + `StockAdjustmentPort`)
- Loads `Sku`, adjusts quantity by delta or sets absolute value
- Used both by order completion and manual adjustment

**`CreateCategoryService.java`** (implements `CreateCategoryUseCase`)
- Simple: validate name unique, persist

### 4.3 New Controller

**`ProductManagementController.java`** (`/api/v1/admin/products`)
- `POST /api/v1/admin/products` → `CreateProductUseCase` (MANAGER + ADMIN)
- `PUT /api/v1/admin/products/{slug}/price` → `UpdateProductPriceUseCase` (ADMIN only)
- `PUT /api/v1/admin/products/{slug}/stock` → `UpdateProductStockUseCase` (ADMIN only)

**`CategoryManagementController.java`** (`/api/v1/admin/categories`)
- `POST /api/v1/admin/categories` → `CreateCategoryUseCase` (MANAGER + ADMIN)
- `DELETE /api/v1/admin/categories/{id}` → `DeleteCategoryUseCase` (ADMIN only)

### 4.4 New Web DTOs

- `CreateProductRequestDto.java` — name, categoryId, colorId, List<SkuDefinitionDto>
- `SkuDefinitionDto.java` — sizeLabel, price, stockQuantity
- `UpdatePriceRequestDto.java` — price (BigDecimal), currency
- `UpdateStockRequestDto.java` — quantity (absolute) or delta (signed int)
- `CreateCategoryRequestDto.java` — name, parentId (nullable)

### 4.5 Update SecurityConfig
Add ADMIN to allowed roles for new `/api/v1/admin/**` endpoints.
Add `UserRole.ADMIN` to enum (Phase 1 added `SYNC`; now add `ADMIN`).

### 4.6 Definition of Done
`POST /api/v1/admin/products` creates a product with variants and SKUs.
Product appears in `GET /api/v1/listings` response. `./mvnw test` passes.

---

## Phase 5 — User Self-Registration

**Goal:** A new user can sign up via phone OTP without being pre-seeded by an external system.
This is the smallest phase — one conditional in one service.

### 5.1 Change to `VerifyOtpService.java`

Current logic:
1. Verify OTP code
2. Load user by phone — throws if not found
3. Issue JWT

New logic:
1. Verify OTP code
2. Load user by phone — **if not found, auto-create** with:
   - `role = USER`
   - `enabled = true`
   - `name = null` (user fills in profile later)
   - `loyalty = LoyaltyProfile.empty()`
3. Issue JWT

### 5.2 Implement `RegisterUserUseCase`

**`RegisterUserService.java`** — extracted from step 2 above so it can be called independently.

### 5.3 Definition of Done
A phone number not in the DB can complete OTP flow and receive a JWT.
User appears in `GET /api/v1/users/me` with `role = USER`.

---

## Phase 6 — Cart Implementation

**Goal:** Full cart lifecycle — add, update, remove, clear — with stock validation and
price snapshots at time of add.

### 6.1 Database Migration

**`V3__cart_tables.sql`:**
```sql
CREATE TABLE carts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    id                  BIGSERIAL PRIMARY KEY,
    cart_id             BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    sku_id              BIGINT NOT NULL REFERENCES skus(id),
    quantity            INT NOT NULL CHECK (quantity > 0),
    unit_price_snapshot NUMERIC(12,2) NOT NULL,  -- price at time of adding
    added_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (cart_id, sku_id)
);
```

### 6.2 New JPA Entities

- `CartEntity.java` — maps `carts` table; OneToMany `cartItems`
- `CartItemEntity.java` — maps `cart_items` table; ManyToOne cart, ManyToOne sku

### 6.3 New Spring Data Repositories

- `CartRepository.java` — `findByUserIdAndStatus(Long userId, String status)`
- `CartItemRepository.java` — `findByCartId(Long cartId)`

### 6.4 New MapStruct Mapper

- `CartMapper.java` — `CartEntity` ↔ `Cart`, `CartItemEntity` ↔ `CartItem`

### 6.5 New Adapter

**`CartRepositoryAdapter.java`** (implements `LoadCartPort`, `SaveCartPort`)
- `findActiveByUserId(Long userId)` — query by userId + status=ACTIVE
- `save(Cart cart)` — map to entity, persist

### 6.6 New Services

- `AddToCartService.java` — load/create active cart, validate sku exists and in stock, snapshot price, call `cart.addItem()`
- `RemoveFromCartService.java`
- `UpdateCartItemQuantityService.java` — validates requested quantity ≤ available stock
- `ClearCartService.java`
- `GetCartService.java` — returns cart with live stock availability flag per item

### 6.7 New Controller

**`CartController.java`** (`/api/v1/cart`)
- `GET /api/v1/cart` → `GetCartUseCase` (USER)
- `POST /api/v1/cart/items` → `AddToCartUseCase` (USER)
- `PUT /api/v1/cart/items/{skuId}` → `UpdateCartItemQuantityUseCase` (USER)
- `DELETE /api/v1/cart/items/{skuId}` → `RemoveFromCartUseCase` (USER)
- `DELETE /api/v1/cart` → `ClearCartUseCase` (USER)

### 6.8 New Web DTOs

- `CartDto.java` — id, items, totalAmount, itemCount
- `CartItemDto.java` — skuId, productName, colorName, sizeLabel, quantity, unitPrice, lineTotal, inStock
- `AddToCartRequestDto.java` — skuId, quantity
- `UpdateCartItemRequestDto.java` — quantity

### 6.9 Definition of Done
Full cart CRUD works via API. Cart total is accurate. Items out of stock cannot be added.
Cart is user-scoped (no cross-user access). `./mvnw test` passes.

---

## Phase 7 — Native Order Creation & Checkout

**Goal:** The app creates orders natively from a cart. Orders no longer require external sync.
The sync import path for orders remains for migration/legacy, but new orders originate here.

### 7.1 Database Migration

**`V4__make_external_order_id_nullable.sql`:**
```sql
ALTER TABLE orders ALTER COLUMN external_order_id DROP NOT NULL;
```
(This is already nullable if the V1 reset in Phase 1 was done correctly.)

### 7.2 New Services

**`CheckoutService.java`** (implements `CheckoutUseCase`)
Steps:
1. Load user's active `Cart` — throw if empty
2. Validate all cart items are still in stock (re-check at checkout time)
3. Snapshot final prices from cart items
4. If `loyaltyPointsToRedeem > 0`: validate points available, apply discount
5. Create `Order` with status `PENDING`, `externalOrderId = null`
6. Persist order via `SaveOrderPort`
7. Decrement stock for each item via `StockAdjustmentPort`
8. Clear cart (set status to CHECKED_OUT)
9. Return orderId

**`UpdateOrderStatusService.java`** (implements `UpdateOrderStatusUseCase`)
- ADMIN only: transition between statuses with validation (PENDING→PAID→SHIPPED→DELIVERED)
- On CANCELLED: restore stock via `StockAdjustmentPort`

**`CancelOrderService.java`** (implements `CancelOrderUseCase`)
- USER: can cancel only if status is PENDING
- ADMIN: can cancel at any non-final status
- Restore stock on cancel

### 7.3 Controller Updates

**Update `OrderController.java`:**
- `POST /api/v1/orders/checkout` → `CheckoutUseCase` (USER)
  - Body: `CheckoutRequestDto` (loyaltyPointsToRedeem, notes)
  - Returns: `CheckoutResponseDto` (orderId, totalAmount, discountApplied)
- `PATCH /api/v1/orders/{id}/cancel` → `CancelOrderUseCase` (USER + ADMIN)
- `PATCH /api/v1/orders/{id}/status` → `UpdateOrderStatusUseCase` (ADMIN only)

### 7.4 New Web DTOs

- `CheckoutRequestDto.java` — loyaltyPointsToRedeem (int, default 0), notes (nullable)
- `CheckoutResponseDto.java` — orderId, status, subtotal, discount, total, items

### 7.5 Definition of Done
Full checkout flow: add to cart → POST /checkout → order created with PENDING status.
Stock decrements correctly. Cart is cleared. Cancellation restores stock.
`./mvnw test` passes.

---

## Phase 8 — Payment Gateway Integration

**Goal:** Real payment processing. Orders transition from PENDING to PAID via a payment
provider. Stubs remain for dev/test.

### 8.1 Database Migration

**`V5__payments_table.sql`:**
```sql
CREATE TABLE payments (
    id                      BIGSERIAL PRIMARY KEY,
    order_id                BIGINT NOT NULL REFERENCES orders(id),
    amount                  NUMERIC(12,2) NOT NULL,
    currency                VARCHAR(10) NOT NULL DEFAULT 'TJS',
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider                VARCHAR(50) NOT NULL,
    provider_transaction_id VARCHAR(255),
    provider_redirect_url   TEXT,
    webhook_payload         TEXT,  -- raw provider callback for audit
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_provider_tx ON payments(provider_transaction_id);
```

### 8.2 New JPA Entity + Repository

- `PaymentEntity.java`
- `PaymentRepository.java` — `findByOrderId`, `findByProviderTransactionId`

### 8.3 New Adapters

- `PaymentRepositoryAdapter.java` (implements `SavePaymentPort`, `LoadPaymentPort`)
- **`PaymePaymentAdapter.java`** or **`ClickPaymentAdapter.java`** (implements `PaymentPort`)
  - Whichever provider is confirmed — decision made at start of this phase
  - `PaymentPortStub.java` from Phase 3 covers dev/test

### 8.4 New Services

**`InitiatePaymentService.java`** (implements `InitiatePaymentUseCase`)
1. Verify order is in PENDING status
2. Call `PaymentPort.initiate(amount, orderId, userId)`
3. Persist `Payment` with status PENDING + provider redirect URL
4. Return redirect URL to client

**`ConfirmPaymentService.java`** (implements `ConfirmPaymentUseCase`)
1. Validate webhook signature
2. Find payment by `providerTransactionId`
3. Update payment status to COMPLETED
4. Transition order to PAID via `UpdateOrderStatusUseCase`
5. Trigger `AwardLoyaltyPointsUseCase` (wired in Phase 9, stubbed here)

**`RefundPaymentService.java`** (implements `RefundPaymentUseCase`)
1. Call `PaymentPort.refund(transactionId, amount)`
2. Update payment status to REFUNDED
3. Transition order to CANCELLED
4. Restore stock

### 8.5 New Controllers

**`PaymentController.java`** (`/api/v1/payments`)
- `POST /api/v1/payments/initiate/{orderId}` → `InitiatePaymentUseCase` (USER, must own order)
  - Returns: `{ redirectUrl, paymentId }`
- `GET /api/v1/payments/{orderId}` → `LoadPaymentPort` directly (USER, status check)
- `POST /api/v1/payments/{orderId}/refund` → `RefundPaymentUseCase` (ADMIN)

**`PaymentWebhookController.java`** (`/api/v1/webhooks`)
- `POST /api/v1/webhooks/payment` → `ConfirmPaymentUseCase` (no auth, signature validated in service)
- Add to SecurityConfig permit-all list (webhooks are public + self-validating)

### 8.6 New Web DTOs

- `InitiatePaymentResponseDto.java` — paymentId, redirectUrl, expiresAt
- `PaymentStatusDto.java` — paymentId, status, provider, amount

### 8.7 Definition of Done
checkout → initiate payment → (stub) confirm → order transitions to PAID.
Webhook endpoint is reachable without JWT. `./mvnw test` passes including webhook flow.

---

## Phase 9 — Native Loyalty Calculation

**Goal:** Loyalty points are calculated and awarded by the app after every successful payment.
Tier upgrades happen automatically. No external system involvement.

### 9.1 Implement `AwardLoyaltyPointsService.java`

Uses `LoyaltyCalculator` domain service (built in Phase 2):
1. Load user with current `LoyaltyProfile`
2. Load all `LoyaltyTier` definitions
3. Call `LoyaltyCalculator.awardPoints(profile, orderAmount, tiers)`
4. If tier changed: update user's `loyaltyTier` FK
5. Persist updated user via `SaveUserPort`

Called from `ConfirmPaymentService` after order transitions to PAID.

### 9.2 Implement `RedeemLoyaltyPointsService.java`

Wired into `CheckoutService` (Phase 7 stubbed this call):
1. Validate user has enough points
2. Calculate monetary value: `LoyaltyCalculator.pointsToMoney(pointsToRedeem)`
3. Apply as discount on order total
4. Deduct points from user profile pessimistically (before payment — prevent double spend)
5. If payment fails, restore points (compensating action in `ConfirmPaymentService`/`RefundPaymentService`)

### 9.3 Update `CheckoutService.java` (Phase 7)

Wire the actual `RedeemLoyaltyPointsService` call (remove the stub).

### 9.4 Expose Loyalty Balance in `UserController`

Update `GET /api/v1/users/me` response to include full `LoyaltyProfile`:
- current points
- tier name + discount %
- spend to next tier
- recent points earned (last 5 orders)

### 9.5 Definition of Done
Full loyalty loop: checkout → pay → points awarded → tier recalculated → balance shown in profile.
Point redemption reduces order total. `LoyaltyCalculatorTest` has ≥ 8 passing test cases.

---

## Phase 10 — Decommission ERP Integration

**Goal:** Delete every trace of the external import layer. The app is fully standalone.

### 10.1 Delete Packages & Files

**Delete entire directory:** `infrastructure/importer/` (renamed from `erp/` in Phase 1)
- All files including `batch/` subdirectory

**Delete** all Sync use cases from `application/ports/in/sync/`:
- `SyncProductHierarchyUseCase.java`
- `SyncUsersUseCase.java`
- `SyncOrdersUseCase.java`
- `SyncLoyaltyPointsUseCase.java`
- `SyncLoyaltyTiersUseCase.java`
- `SyncCategoriesUseCase.java`
- `SyncDiscountUseCase.java`
- `RemoveDiscountUseCase.java`

**Delete** corresponding service implementations in `application/services/`:
- All `Sync*Service.java` files

**Delete** the sync controller and DTOs:
- `ImportController.java` (was `ErpSyncController`)
- All `Import*Dto.java` web DTOs (was `Sync*Dto`)

**Delete** sync log infrastructure:
- `SyncLogEntity.java`
- `SyncLogRepository.java`
- `SyncLogAdapter.java`
- `LogImportEventPort.java` (out-port)

**Delete** Spring Batch dependency from `pom.xml` and all Batch config:
- `batch/ImportJobConfig.java`
- `batch/ImportJobScheduler.java`
- Remove `spring-boot-starter-batch` from pom.xml

### 10.2 Clean Up Roles

- Remove `UserRole.SYNC`
- Update `SecurityConfig.java` — remove all `SYNC` role references
- The `ApiKeyAuthenticationFilter.java` — **repurpose, not delete**: rename to
  `ServiceApiKeyFilter.java`, keep for payment webhook signature validation or admin tooling

### 10.3 Database Migration

**`V6__drop_sync_log_table.sql`:**
```sql
DROP TABLE IF EXISTS sync_log;
DROP TABLE IF EXISTS idempotency_records; -- only if not reused for payments
```

Note: Keep `idempotency_records` if it's being reused for payment webhook idempotency.

### 10.4 Clean Up `IdempotencyPort`

If kept for payment webhooks: rename to `WebhookIdempotencyPort`, update adapter.
If not needed: delete port + entity + adapter.

### 10.5 Remove Spring Batch from pom.xml

Remove:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
```
And the Spring Batch schema migration (`V4__spring_batch_schema.sql` equivalent in new schema).

### 10.6 Update CLAUDE.md

Rewrite the `CLAUDE.md` "Project Context" section:
- Remove "ERPNext is the SOURCE OF TRUTH" statement
- Update `enrichWithErpData()` reference
- Update `SYSTEM` role reference
- Reflect that Radolfa is now the authoritative source for all data

### 10.7 Definition of Done
`./mvnw test` passes with zero references to "erp", "sync", or "import" in production code paths.
`grep -r "erp\|Erp\|ERP" backend/src/main/java` returns zero results.
App starts cleanly. Full e-commerce flow (register → browse → cart → checkout → pay → loyalty) works end to end.

---

## Quick Reference: New Domain Objects by Phase

| Phase | New Domain Model | Type |
|-------|-----------------|------|
| 2 | `Cart` | Aggregate Entity |
| 2 | `CartItem` | Entity |
| 2 | `CartStatus` | Enum |
| 2 | `Payment` | Record |
| 2 | `PaymentStatus` | Enum |
| 2 | `LoyaltyCalculator` | Domain Service |

## Quick Reference: New Use Cases by Phase

| Phase | Use Case | Who Calls It |
|-------|----------|-------------|
| 4 | `CreateProductUseCase` | MANAGER/ADMIN |
| 4 | `UpdateProductPriceUseCase` | ADMIN |
| 4 | `UpdateProductStockUseCase` | System (post-order) + ADMIN |
| 5 | `RegisterUserUseCase` | Internal (OTP verify) |
| 6 | `AddToCartUseCase` | USER |
| 6 | `GetCartUseCase` | USER |
| 6 | `RemoveFromCartUseCase` | USER |
| 6 | `ClearCartUseCase` | USER |
| 7 | `CheckoutUseCase` | USER |
| 7 | `CancelOrderUseCase` | USER + ADMIN |
| 7 | `UpdateOrderStatusUseCase` | ADMIN |
| 8 | `InitiatePaymentUseCase` | USER |
| 8 | `ConfirmPaymentUseCase` | Webhook (internal) |
| 8 | `RefundPaymentUseCase` | ADMIN |
| 9 | `AwardLoyaltyPointsUseCase` | Internal (post-payment) |
| 9 | `RedeemLoyaltyPointsUseCase` | Internal (checkout) |

## Quick Reference: New Database Tables by Phase

| Phase | Table | Purpose |
|-------|-------|---------|
| 6 | `carts` | Active shopping carts |
| 6 | `cart_items` | Items in a cart with price snapshot |
| 8 | `payments` | Payment records with provider data |

---

*Last updated: 2026-03-19*
