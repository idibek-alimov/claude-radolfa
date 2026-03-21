# Architecture

**Analysis Date:** 2026-03-21

## Pattern Overview

**Overall:** Hexagonal Architecture (Ports & Adapters) on the backend, Feature-Sliced Design (FSD) on the frontend. Both are strictly enforced.

**Key Characteristics:**
- Domain layer is pure Java — zero Spring, JPA, Jackson, or Lombok dependencies
- Use cases are single-responsibility interfaces; one service class per use case
- Frontend dependency direction is strictly downward: `app → views/pages → widgets → features → entities → shared`
- All cross-layer communication goes through explicitly declared Port interfaces
- MapStruct handles all DTO ↔ Domain ↔ JPA Entity mappings

---

## Backend Layers

**Domain (`tj.radolfa.domain`):**
- Purpose: Core business logic and invariants. The only layer with no external dependencies.
- Location: `backend/src/main/java/tj/radolfa/domain/`
- Contains: Domain models (`Cart`, `Order`, `ProductBase`, `Sku`, `User`, `Money`), value objects, domain exceptions, domain services (`LoyaltyCalculator`), utilities (`SlugUtils`)
- Domain models are mutable classes when they have business behaviour (e.g., `Cart`, `ProductBase`); immutable records for value objects (e.g., `Order`, `OrderItem`, `Money`)
- Depends on: Nothing
- Used by: Application layer

**Application (`tj.radolfa.application`):**
- Purpose: Orchestrates use cases by coordinating domain models through port interfaces
- Location: `backend/src/main/java/tj/radolfa/application/`
- Contains:
  - `ports/in/`: Use case interfaces (e.g., `CheckoutUseCase`, `CreateProductUseCase`)
  - `ports/out/`: Repository and adapter interfaces (e.g., `LoadCartPort`, `SaveOrderPort`, `PaymentPort`)
  - `services/`: One `@Service` class per use case, implementing the corresponding In-Port
  - `readmodel/`: Read-only projection DTOs used for queries (e.g., `ListingVariantDto`, `SkuDto`)
- Depends on: Domain
- Used by: Infrastructure (web controllers call In-Ports; persistence adapters implement Out-Ports)

**Infrastructure (`tj.radolfa.infrastructure`):**
- Purpose: Adapters connecting the application to external systems
- Location: `backend/src/main/java/tj/radolfa/infrastructure/`
- Sub-packages:
  - `web/`: REST controllers, DTOs, `SecurityConfig`, `GlobalExceptionHandler`, `TierPricingEnricher`
  - `persistence/`: JPA entities, Spring Data repositories, persistence adapters, MapStruct mappers
  - `security/`: JWT filter, OTP store, rate limiter, API key filter, auth cookie manager
  - `s3/`: S3 image uploader and thumbnail processor (Thumbnailator)
  - `search/`: Elasticsearch listing document and search adapter
  - `scheduling/`: `MonthlyTierEvaluationJob` — scheduled tier re-evaluation
  - `payment/`: Payment gateway adapter
  - `notification/`: Notification adapter (OTP delivery)
  - `config/`: Spring configuration beans
- Depends on: Application (ports), Spring Boot, JPA, AWS SDK, etc.
- Used by: Nothing (outermost ring)

---

## Frontend Layers (FSD)

**`app/` (Routing layer):**
- Purpose: Next.js App Router entry points only. No business logic or direct feature imports.
- Location: `frontend/src/app/`
- Contains: `page.tsx`, `layout.tsx` files only. Route groups: `(storefront)`, `(admin)`.

**`views/` (Page composition):**
- Purpose: Page-level UI composition. Imports from widgets only.
- Location: `frontend/src/views/`
- Contains: `checkout/`, `payment/return/` — complex page components that don't fit in `app/`

**`widgets/` (Complex UI blocks):**
- Purpose: Self-contained, multi-feature UI sections
- Location: `frontend/src/widgets/`
- Examples: `Navbar/`, `CartDrawer/`, `ProductList/`, `HomeCollections/`, `MegaMenu/`, `HeroBanner/`, `Footer/`, `TrustBanner/`, `loyalty-dashboard/`
- Internal structure: `ui/`, `api/`, `model/` subdirectories as needed

**`features/` (User actions):**
- Purpose: User-triggered actions with side effects (mutations, navigation)
- Location: `frontend/src/features/`
- Examples: `auth/`, `cart/`, `checkout/`, `payment/`, `search/`, `product-creation/`, `product-edit/`, `loyalty/`, `profile/`, `user-management/`
- Internal structure: `ui/`, `api/`, `model/`, `hooks/` subdirectories as needed

**`entities/` (Business domain types + read-only UI):**
- Purpose: Domain types, API query hooks, and read-only presentation components
- Location: `frontend/src/entities/`
- Examples: `product/`, `cart/`, `category/`, `user/`, `loyalty/`, `color/`
- Internal structure: `api/`, `model/types.ts`, `ui/` subdirectories. Each entity exposes a barrel `index.ts`.

**`shared/` (Infrastructure and UI kit):**
- Purpose: Shared utilities, HTTP client, Shadcn UI components, i18n config
- Location: `frontend/src/shared/`
- Contains: `api/` (Axios instance at `shared/api/axios.ts`), `ui/` (Shadcn components), `lib/`, `components/`, `providers/`, `i18n/`

---

## Data Flow

**Read: Product Listing with Loyalty Pricing**

1. HTTP GET `/api/v1/listings` arrives at `ListingController`
2. Controller calls `GetListingUseCase.execute(query)`
3. `GetListingService` calls `LoadListingPort` → `ListingReadAdapter` → JDBC/JPA query → returns `PageResult<ListingVariantDto>`
4. Controller calls `TierPricingEnricher.enrich(page)` which reads JWT principal from SecurityContext, calls `ResolveUserDiscountUseCase`, and stamps loyalty pricing onto each DTO
5. Controller serializes enriched `PageResult<ListingVariantDto>` to JSON

**Write: Checkout Flow**

1. HTTP POST `/api/v1/orders/checkout` arrives at `OrderController`
2. Controller extracts `JwtAuthenticatedUser` from `@AuthenticationPrincipal`
3. Controller calls `CheckoutUseCase.execute(Command)` → `CheckoutService`
4. `CheckoutService` loads `Cart` via `LoadCartPort`, validates stock via `LoadSkuPort`, computes pricing with `LoyaltyCalculator`, optionally redeems loyalty points via `RedeemLoyaltyPointsUseCase`
5. Persists `Order` via `SaveOrderPort`, decrements stock via `StockAdjustmentPort`, transitions cart to `CHECKED_OUT` via `SaveCartPort`
6. Returns `CheckoutUseCase.Result` record → controller maps to `CheckoutResponseDto`

**Frontend: TanStack Query Data Flow**

1. Page component renders; widget or feature mounts with `useQuery`
2. `useQuery` calls API function from `entities/{domain}/api/` using the shared Axios instance at `shared/api/axios.ts`
3. On 401 response, Axios interceptor auto-refreshes token via `/api/v1/auth/refresh` and retries
4. Query result populates UI; mutations invalidate relevant query keys defined per entity

**Authentication Flow**

1. User submits phone → `POST /api/v1/auth/otp/send` → OTP delivered via `NotificationPort`
2. User submits OTP → `POST /api/v1/auth/verify` → backend issues JWT as HTTP-only cookie
3. All subsequent requests include cookie automatically (`withCredentials: true`)
4. `JwtAuthenticationFilter` validates cookie on every request; sets `JwtAuthenticatedUser` principal

---

## Key Abstractions

**Use Case Interfaces (`ports/in`):**
- Purpose: Define the application boundary for each discrete operation
- Pattern: Interface with nested `Command` record (for writes) and `Result` record where needed
- Examples: `backend/src/main/java/tj/radolfa/application/ports/in/order/CheckoutUseCase.java`, `backend/src/main/java/tj/radolfa/application/ports/in/product/CreateProductUseCase.java`

**Output Ports (`ports/out`):**
- Purpose: Abstract persistence, external APIs, and infrastructure from the application
- Pattern: Single-method or small focused interfaces (e.g., `LoadCartPort`, `SaveOrderPort`, `PaymentPort`, `ImageUploadPort`)
- Location: `backend/src/main/java/tj/radolfa/application/ports/out/`

**Read Model (`application/readmodel`):**
- Purpose: Flat projection DTOs for efficient reads — avoids loading full aggregate graphs
- Pattern: Java `record` types used directly from persistence adapters through to controllers
- Examples: `ListingVariantDto`, `SkuDto`, `ListingVariantDetailDto`, `HomeSectionDto`, `CartView`
- Location: `backend/src/main/java/tj/radolfa/application/readmodel/`

**Domain Models:**
- `Cart` — mutable aggregate, enforces `CartStatus.ACTIVE` before any mutation
- `ProductBase` — mutable class with named mutation methods (`applyExternalUpdate`, `updateCategory`)
- `Order`, `OrderItem`, `Money` — immutable `record` types
- `Money` — value object wrapping `BigDecimal`; enforces non-null amounts
- Location: `backend/src/main/java/tj/radolfa/domain/model/`

**TierPricingEnricher:**
- Purpose: Request-scoped component that resolves the current user's loyalty tier discount and stamps it onto read model DTOs at the web layer
- Location: `backend/src/main/java/tj/radolfa/infrastructure/web/TierPricingEnricher.java`

---

## Entry Points

**Backend — Spring Boot Application:**
- Location: `backend/src/main/java/tj/radolfa/RadolfaApplication.java`
- Triggers: JVM startup via `./mvnw spring-boot:run`
- Responsibilities: Loads Spring context, starts embedded Tomcat on port 8080

**Backend — REST Controllers:**
- Location: `backend/src/main/java/tj/radolfa/infrastructure/web/`
- Controllers: `AuthController`, `CartController`, `OrderController`, `PaymentController`, `PaymentWebhookController`, `ListingController`, `ProductManagementController`, `CategoryController`, `CategoryManagementController`, `ColorController`, `DiscountController`, `DiscountTypeController`, `HomeController`, `SearchController`, `LoyaltyController`, `UserController`
- Base path: `/api/v1`

**Backend — Payment Webhook:**
- Location: `backend/src/main/java/tj/radolfa/infrastructure/web/PaymentWebhookController.java`
- Triggers: External payment gateway callback to `/api/v1/webhooks/payment`
- Public endpoint, no auth required

**Backend — Scheduled Jobs:**
- Location: `backend/src/main/java/tj/radolfa/infrastructure/scheduling/MonthlyTierEvaluationJob.java`
- Triggers: Cron schedule — evaluates monthly spending and reassigns loyalty tiers

**Frontend — Next.js App Router:**
- Location: `frontend/src/app/layout.tsx` (root), `frontend/src/app/(storefront)/layout.tsx`, `frontend/src/app/(admin)/layout.tsx`
- Triggers: `npm run dev --prefix frontend` or Next.js server startup

---

## Error Handling

**Strategy:** Centralized at the web layer via `GlobalExceptionHandler`; domain exceptions bubble up through application services unchanged.

**Patterns:**
- Domain invariant violations: throw `IllegalStateException` or `IllegalArgumentException` → mapped to 422/400 by `GlobalExceptionHandler`
- Role/access violations: `AccessDeniedException` → 403; `AuthenticationException` → 401
- Field lock violations: `FieldLockException` (domain exception) → 403
- Image processing errors: `ImageProcessingException` (domain exception) → 422
- Concurrent modification: `OptimisticLockException` → 409
- Duplicate data: `DataIntegrityViolationException` → 409
- `@Valid` bean validation failures → 400 with field-level error map
- All unhandled exceptions → 500 with generic message (error logged at ERROR level)

**Frontend:**
- Axios interceptor in `frontend/src/shared/api/axios.ts` handles 401 → auto-refresh → retry
- On refresh failure: shows toast via `sonner` and redirects to `/login`
- Error messages read from `err.response.data.message` via `getErrorMessage()` utility

---

## Cross-Cutting Concerns

**Logging:** SLF4J via `LoggerFactory` in `GlobalExceptionHandler`; log level prefixes `[SECURITY]`, `[VALIDATION]`, `[STATE]`, `[ERROR]`, `[IMAGE]`, `[FIELD-LOCK]`, `[CONFLICT]`, `[CONSTRAINT]`

**Validation:** Bean Validation (`@Valid`) on controller request DTOs; domain-level validation via constructor guards and named mutation methods

**Authentication:** Stateless JWT via HTTP-only cookie; `JwtAuthenticationFilter` validates on every request; `ServiceApiKeyFilter` for service-to-service calls; `@EnableMethodSecurity` enables method-level `@PreAuthorize`

**Database Migrations:** Flyway — migration files in `backend/src/main/resources/db/migration/`; dev seed data in `backend/src/main/resources/db/migration-dev/`

**Search:** Elasticsearch via Spring Data Elasticsearch; `ListingSearchAdapter` implements `SearchListingPort`; stub implementation (`ListingSearchStub`) available for non-Elasticsearch environments

**Image Processing:** Java resizes uploads to 150×150, 400×400, 800×800 via `ThumbnailatorImageProcessor`; uploads to S3 via `S3ImageUploader`; stub (`S3ImageUploaderStub`) for local dev

---

*Architecture analysis: 2026-03-21*
