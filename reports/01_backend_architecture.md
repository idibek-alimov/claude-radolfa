# Radolfa Backend — Architecture Overview

> Generated: 2026-03-20
> Commit range: `824f7cc` (before) → `58ffab0` (after, Phase 10)

---

## 1. What It Is

Radolfa is a **standalone e-commerce platform** backend. It is a **Spring Boot 3.2.3 hexagonal monolith** written in Java 17. After Phase 10, it is fully autonomous — it no longer polls or syncs from ERPNext. Radolfa is the **single source of truth** for all products, prices, stock, orders, and users.

---

## 2. Technology Stack

| Concern | Technology |
|---|---|
| Framework | Spring Boot 3.2.3 |
| Java Version | 17 (records, sealed types) |
| Database | PostgreSQL 16 (Flyway migrations) |
| Persistence | Spring Data JPA + Hibernate 6 |
| Search | Elasticsearch |
| Image Processing | Thumbnailator (Java-side resize → S3) |
| Object Storage | AWS S3-compatible (Timeweb Cloud) |
| Authentication | JWT (JJWT) — OTP-based login |
| Code Generation | MapStruct (mappings) + Lombok (boilerplate) |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Build | Maven (use `./mvnw`) |

---

## 3. Hexagonal Architecture

The codebase enforces a strict three-layer hexagonal (ports & adapters) structure:

```
tj.radolfa
├── domain/            ← Pure Java. Zero Spring, Zero JPA. Business rules only.
├── application/       ← Business logic. Depends only on domain.
│   ├── ports/in/      ← Use case interfaces (what the system does)
│   ├── ports/out/     ← Repository/adapter interfaces (what the system needs)
│   └── services/      ← Implementations of in-ports, use out-ports via DI
└── infrastructure/    ← All framework / technology specifics
    ├── web/           ← REST controllers, request/response DTOs
    ├── persistence/   ← JPA entities, Spring Data repos, adapters
    ├── security/      ← JWT filter, Spring Security config
    ├── s3/            ← Image upload / processing
    └── search/        ← Elasticsearch indexing
```

**Dependency direction:** `infrastructure` → `application` → `domain`. The domain knows nothing about Spring, JPA, or HTTP.

---

## 4. Domain Layer

### Value Objects

| Type | Description |
|---|---|
| `Money` | Immutable `BigDecimal` wrapper. Validates non-negative. Factory: `Money.of()`. Operations: `add()`, `multiply()`. |
| `PhoneNumber` | User identifier. Unique, required. |
| `UserRole` | Enum: `USER`, `MANAGER`, `ADMIN`. |
| `ProductAttribute` | Key-value pair with sort order for product metadata. |

### Aggregate Roots

**`User`** — Immutable record.
```
id, phone (PhoneNumber), role (UserRole), name, email,
loyalty (LoyaltyProfile), enabled, version (optimistic lock)
```
- No mutation methods. Profile updates build a new `User` record.

**`Cart`** — Mutable class (domain methods enforce invariants).
```
id, userId, status (ACTIVE / CHECKED_OUT / ABANDONED)
items: List<CartItem>
```
Mutation methods: `addItem()`, `removeItem()`, `updateQuantity()`, `checkout()`, `abandon()`.
Private guard: `requireActive()` — all mutations fail if cart is not ACTIVE.
`touch()` updates `updatedAt` on every mutation.

**`CartItem`** — Immutable. Captures `unitPrice` snapshot at add-time (price changes after adding do not affect the cart).

**`Sku`** — Mutable class. The only stock/price unit in the system.
```
id, listingVariantId, skuCode (unique, immutable), sizeLabel, stockQuantity, price
```
Single authorized mutation path: `updatePriceAndStock(Money, Integer)`.
Price and stock are always **fully overwritten** (no partial updates).

**`ListingVariant`** — Color variant of a product. Contains a list of `Sku`s.
Holds web enrichment: description, images, attributes, topSelling flag, featured flag.

**`ProductBase`** — Product template/umbrella. Has many `ListingVariant`s. Belongs to a `Category`.

**`Order`** — Immutable record.
Status lifecycle: `PENDING → PAID → SHIPPED → DELIVERED`.
`OrderItem`s snapshot SKU code, product name, quantity, and price at order time.

**`Payment`** — Immutable record. Links to an `Order`. Holds provider transaction ID for webhook matching.
Status: `PENDING → COMPLETED → REFUNDED`.

**`Discount`** — Pricing rule. Applies to specific SKU codes. Has `validFrom`/`validUpto` period and a `disabled` flag.

**`LoyaltyProfile`** — Embedded value object on `User`.
```
tier (LoyaltyTier), points, spendToNextTier, spendToMaintainTier, currentMonthSpending
```

**`LoyaltyTier`** — Defines thresholds and rates.
```
minSpendRequirement, discountPercentage, cashbackPercentage, displayOrder
```

### Domain Services

**`LoyaltyCalculator`** — Stateless, pure Java. No Spring beans.

| Method | Description |
|---|---|
| `awardPoints(profile, orderAmount, allTiers)` | Computes cashback points, updates monthly spend, determines tier. |
| `resolveDiscount(profile, orderTotal)` | Returns tier discount as a `Money` amount. |
| `maxRedeemablePoints(profile, orderTotal)` | Returns `min(available points, 30% of order total)`. |
| `pointsToMoney(int points)` | Converts points to `Money` — 1 point = 1 TJS. |

### Domain Exceptions

| Exception | When |
|---|---|
| `FieldLockException` | Attempt to mutate a protected field (e.g., price by MANAGER). |
| `ErpLockViolationException` | (Legacy) ERP sync constraint violation. |
| `ImageProcessingException` | Image resize or upload failure. |

---

## 5. Application Layer

### In-Ports (Use Cases)

**Cart**
- `AddToCartUseCase` — Load SKU, validate stock, find/create active cart, add item.
- `GetCartUseCase` — Return enriched cart view.
- `RemoveFromCartUseCase`, `UpdateCartItemQuantityUseCase`, `ClearCartUseCase`.

**Order**
- `CheckoutUseCase` — Full checkout flow (see Section 8).
- `CreateOrderUseCase`, `UpdateOrderStatusUseCase`, `CancelOrderUseCase`.

**Payment**
- `InitiatePaymentUseCase` — Create `Payment` (PENDING), return provider URL.
- `ConfirmPaymentUseCase` — Webhook handler: mark COMPLETED, trigger loyalty award.
- `RefundPaymentUseCase` — Reverse payment, restore loyalty points.

**Loyalty**
- `AwardLoyaltyPointsUseCase` — Award cashback after successful payment.
- `RedeemLoyaltyPointsUseCase` — Pessimistically deduct points before payment.

**Product**
- `CreateProductUseCase`, `UpdateProductPriceUseCase` (ADMIN), `UpdateProductStockUseCase` (ADMIN).
- `CreateCategoryUseCase`, `DeleteCategoryUseCase`.

**User / Auth**
- `RegisterUserUseCase`, `ChangeUserRoleUseCase`, `ToggleUserStatusUseCase`, `UpdateUserProfileUseCase`.
- `SendOtpUseCase`, `VerifyOtpUseCase`.

### Out-Ports (Adapter Contracts)

The application layer talks to infrastructure only through these interfaces:

**Load Ports** (queries): `LoadUserPort`, `LoadCartPort`, `LoadSkuPort`, `LoadProductBasePort`, `LoadListingVariantPort`, `LoadOrderPort`, `LoadPaymentPort`, `LoadCategoryPort`, `LoadDiscountPort`, `LoadLoyaltyTierPort`, `LoadColorPort`, `LoadHomeCollectionsPort`, `SearchListingPort`, `SearchUsersPort`.

**Save Ports** (mutations): `SaveUserPort`, `SaveCartPort`, `SaveOrderPort`, `SavePaymentPort`, `SaveProductHierarchyPort`, `SaveCategoryPort`, `SaveDiscountPort`, `SaveListingVariantPort`, `SaveColorPort`, `SaveLoyaltyTierPort`.

**Specialized Ports**: `StockAdjustmentPort` (decrement on checkout), `ListingIndexPort` (Elasticsearch), `IdempotencyPort` (payment dedup), `PaymentPort` (external provider), `NotificationPort` (email/SMS), `OtpPort` (OTP delivery), `TokenIssuerPort` (JWT).

---

## 6. Infrastructure Layer

### REST Controllers (13 total)

| Controller | Base Path |
|---|---|
| `AuthController` | `/api/v1/auth` |
| `CartController` | `/api/v1/cart` |
| `OrderController` | `/api/v1/orders` |
| `PaymentController` | `/api/v1/payments` |
| `PaymentWebhookController` | `/api/v1/webhooks/payment` |
| `ListingController` | `/api/v1/listings` |
| `ProductManagementController` | `/api/v1/admin/products` |
| `CategoryManagementController` | `/api/v1/admin/categories` |
| `SearchController` | `/api/v1/search` |
| `UserController` | `/api/v1/users` |
| `LoyaltyTierController` | `/api/v1/admin/loyalty-tiers` |
| `ColorController` | `/api/v1/admin/colors` |
| `HomeController` | `/api/v1/home` |

> See Report 02 for the full endpoint breakdown including request/response DTOs.

### Persistence

- **23 Flyway migration files** (`V1__baseline` → `V24__add_product_code`).
- `hibernate.ddl-auto: validate` — Flyway owns the schema.
- One JPA entity per table. Entities never leave the persistence layer; MapStruct converts them to domain objects.

**Core Tables:**

| Table | Purpose |
|---|---|
| `product_base` | Product template |
| `listing_variant` | Color variant of product |
| `sku` | Size/price/stock unit |
| `category` | Hierarchical product categories |
| `color` | Color definitions (colorKey, hexCode) |
| `cart` | Shopping carts |
| `cart_item` | Cart line items (price snapshot) |
| `order` | Orders |
| `order_item` | Order line items (price snapshot) |
| `payment` | Payment records |
| `discount` | Pricing rules |
| `radolfa_user` | Users with embedded loyalty fields |
| `loyalty_tier` | Loyalty program tiers |

### Mappers (MapStruct — compile-time)

One mapper per aggregate: `ProductHierarchyMapper`, `CartMapper`, `OrderMapper`, `PaymentMapper`, `DiscountMapper`, `LoyaltyTierMapper`, `UserMapper`.

Direction: Domain Object ↔ JPA Entity (for persistence) and Domain Object ↔ DTO (for HTTP).

### Security

- **JWT** (15-min access, 7-day refresh). Secret from `JWT_SECRET` env var.
- **OTP login** — phone number → 4-digit OTP (5-min expiry) → JWT pair.
- **`JwtAuthenticationFilter`** — reads from `Authorization: Bearer` header OR `authToken` HTTP-only cookie.
- **`ServiceApiKeyFilter`** — validates `X-Api-Key` header for machine-to-machine calls.
- **`@EnableMethodSecurity`** + `@PreAuthorize` annotations on service methods.
- CSRF disabled (stateless JWT). Sessions: STATELESS.

### Image Processing

- Java resizes on the server (Thumbnailator) to three sizes: 150×150, 400×400, 800×800.
- Uploads to S3-compatible storage (Timeweb Cloud).
- Frontend reads S3 URLs directly. **No image processing on frontend.**

### Search (Elasticsearch)

- `ListingSearchAdapter` indexes `ListingVariant` documents.
- Supports full-text search on name, description, attributes, category, color.
- Admin reindex endpoint rebuilds the entire index.

---

## 7. Security Model (RBAC)

| Role | What They Can Do |
|---|---|
| `USER` | Browse listings, manage own cart, place orders, view own order history, loyalty status. |
| `MANAGER` | All USER permissions + create products, upload/edit images, edit descriptions, enrich listings. **Cannot change price or stock.** |
| `ADMIN` | Full access: everything MANAGER can do + set prices, set stock, manage orders, manage users, manage loyalty tiers, reindex search. |

**Key security rules:**
1. `MANAGER` role is enforced at the service layer — `FieldLockException` is thrown on price/stock write attempts.
2. Points are deducted **before** payment confirmation (pessimistic) to prevent double-spend.
3. Payment webhook is idempotency-guarded — duplicate callbacks are silently ignored.
4. Stock is re-validated at checkout time (not at add-to-cart).

---

## 8. Key Business Flows

### Checkout

```
CheckoutUseCase.execute(userId, loyaltyPointsToRedeem)
  1. Load user + loyalty profile
  2. Load active cart — fail if empty
  3. Re-validate all items have sufficient stock
  4. Compute subtotal from price snapshots (not current prices)
  5. Apply tier discount (LoyaltyCalculator.resolveDiscount)
  6. Apply points redemption (max = min(points, 30% of after-discount total))
  7. Compute final total (floor at 0, cannot go negative)
  8. Build OrderItems with product name / sku code snapshots
  9. Persist Order (status=PENDING)
  10. Decrement stock (StockAdjustmentPort)
  11. Transition cart → CHECKED_OUT
  Returns: orderId, subtotal, tierDiscount, pointsDiscount, total
```

### Payment Confirmation (Webhook)

```
POST /api/v1/webhooks/payment/confirm
  1. Load payment by providerTransactionId
  2. Idempotency guard — skip if already COMPLETED
  3. Payment → COMPLETED
  4. Order → PAID
  5. AwardLoyaltyPointsUseCase:
       - cashback = floor(orderAmount × cashbackPct / 100) points
       - Update monthly spend
       - Tier upgrade if monthly spend crosses threshold
       - Persist updated User
```

### Loyalty Tier Resolution

Tier is assigned based on `currentMonthSpending`. The highest tier whose `minSpendRequirement ≤ currentMonthSpending` wins. Tiers only upgrade; no automatic downgrade logic exists yet.

---

## 9. Phase 10 Changes (What Was Removed)

Phase 10 removed all ERPNext integration:

| Removed | Description |
|---|---|
| `ErpProductClient` | HTTP client that fetched product hierarchy from ERPNext |
| `ErpSyncJobConfig` | Scheduled batch sync jobs |
| `SyncProductHierarchyUseCase` | Synced products/variants/SKUs from ERP |
| `SyncCategoriesUseCase` | Synced category tree from ERP |
| `SyncDiscountUseCase` | Synced pricing rules from ERP |
| `SyncLoyaltyPointsUseCase` | Synced loyalty point balances from ERP |
| `SyncLoyaltyTiersUseCase` | Synced loyalty tier definitions from ERP |
| `SyncOrdersUseCase` | Pushed orders back to ERP |
| `SyncUsersUseCase` | Synced user data from ERP |
| `ROLE_SYSTEM` | Special role for ERP sync — replaced by `ROLE_ADMIN` |

**After Phase 10:** Radolfa owns all data. Products, prices, stock, orders, users, and loyalty are all managed natively through the Radolfa admin UI/API.

---

## 10. Environment Variables (Production)

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://host:5432/radolfa_db
DB_USERNAME=...
DB_PASSWORD=...
ELASTICSEARCH_URIS=http://host:9200
JWT_SECRET=<min-32-chars>
JWT_EXPIRATION_MS=900000          # 15 min
JWT_REFRESH_EXPIRATION_MS=604800000  # 7 days
SYSTEM_API_KEY=<random-secret>
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_S3_BUCKET=96862269-2691-42c6-84ee-15f60b431da7
AWS_S3_REGION=ru-1
AWS_S3_ENDPOINT=https://s3.twcstorage.ru
```

---

## 11. Commands

```bash
# Start backend
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build production JAR
./mvnw clean package -DskipTests

# Start dependencies
docker-compose up -d postgres elasticsearch

# API docs (dev)
http://localhost:8080/swagger-ui.html
```
