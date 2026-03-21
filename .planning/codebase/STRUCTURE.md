# Codebase Structure

**Analysis Date:** 2026-03-21

## Directory Layout

```
claude-radolfa/                        # Project root
├── backend/                           # Spring Boot hexagonal monolith
│   ├── src/main/java/tj/radolfa/
│   │   ├── domain/                    # Pure Java — zero framework deps
│   │   │   ├── model/                 # Domain entities and value objects
│   │   │   ├── service/               # Domain services (LoyaltyCalculator)
│   │   │   ├── exception/             # Domain-specific exceptions
│   │   │   └── util/                  # Domain utilities (SlugUtils)
│   │   ├── application/
│   │   │   ├── ports/
│   │   │   │   ├── in/                # Use case interfaces (grouped by domain)
│   │   │   │   └── out/               # Repository & adapter interfaces
│   │   │   ├── services/              # One @Service class per use case
│   │   │   └── readmodel/             # Read-only projection record DTOs
│   │   └── infrastructure/
│   │       ├── web/                   # REST controllers, DTOs, security config
│   │       │   └── dto/               # Request/response record DTOs
│   │       ├── persistence/
│   │       │   ├── adapter/           # Implements Out-Ports; bridges JPA ↔ domain
│   │       │   ├── entity/            # JPA @Entity classes
│   │       │   ├── mappers/           # MapStruct mappers (Entity ↔ Domain)
│   │       │   └── repository/        # Spring Data JPA repositories
│   │       ├── security/              # JWT filter, OTP, rate limiter, API key
│   │       ├── s3/                    # S3 image uploader + Thumbnailator processor
│   │       ├── search/                # Elasticsearch adapter + document
│   │       ├── scheduling/            # Cron jobs (monthly tier evaluation)
│   │       ├── payment/               # Payment gateway adapter
│   │       ├── notification/          # OTP notification adapter
│   │       └── config/                # Spring @Configuration beans
│   ├── src/main/resources/
│   │   ├── db/migration/              # Flyway migration scripts (V1__, V3__…)
│   │   ├── db/migration-dev/          # Dev seed data migrations
│   │   ├── elasticsearch/             # Elasticsearch index mappings
│   │   └── application.yml            # (implied) Spring Boot config
│   └── src/test/java/tj/radolfa/     # Unit and integration tests
│       ├── domain/                    # Pure domain tests
│       ├── application/services/      # Service layer tests (mocked ports)
│       └── infrastructure/web/        # Controller slice tests
├── frontend/                          # Next.js 15 + FSD
│   └── src/
│       ├── app/                       # Next.js App Router (routing only)
│       │   ├── (storefront)/          # Public storefront route group
│       │   ├── (admin)/               # Admin panel route group
│       │   ├── layout.tsx             # Root layout + providers
│       │   ├── error.tsx              # Global error boundary
│       │   └── not-found.tsx          # 404 page
│       ├── views/                     # Page-level compositions (not in app/)
│       │   ├── checkout/              # Checkout page view
│       │   └── payment/return/        # Payment return page view
│       ├── widgets/                   # Complex self-contained UI blocks
│       │   ├── Navbar/
│       │   ├── CartDrawer/
│       │   ├── ProductList/
│       │   ├── HomeCollections/
│       │   ├── MegaMenu/
│       │   ├── HeroBanner/
│       │   ├── Footer/
│       │   ├── TrustBanner/
│       │   ├── loyalty-dashboard/
│       │   ├── checkout/              # Checkout widget
│       │   └── payment/               # Payment widget
│       ├── features/                  # User-triggered actions with side effects
│       │   ├── auth/                  # OTP login flow
│       │   ├── cart/                  # Add/remove/update cart items
│       │   ├── checkout/              # Checkout submit
│       │   ├── payment/               # Initiate + confirm payment
│       │   ├── search/                # Elasticsearch search UI
│       │   ├── product-creation/      # Admin: create product
│       │   ├── product-edit/          # Admin: edit product fields
│       │   ├── loyalty/               # Loyalty tier display actions
│       │   ├── profile/               # User profile update
│       │   └── user-management/       # Admin: manage users
│       ├── entities/                  # Domain types + read-only UI + query hooks
│       │   ├── product/               # ProductCard, ProductDetail, types, API
│       │   ├── cart/                  # Cart types and API
│       │   ├── category/              # Category API (admin)
│       │   ├── user/                  # User types
│       │   ├── loyalty/               # Loyalty types and API
│       │   └── color/                 # Color API
│       └── shared/                    # Infrastructure and UI kit
│           ├── api/                   # Axios instance (axios.ts), types
│           ├── ui/                    # Shadcn components
│           ├── lib/                   # Utility functions
│           ├── components/            # Generic shared components
│           ├── providers/             # React context providers
│           └── i18n/                  # i18n config and locale files
├── docker-compose.yml                 # Dev: postgres + elasticsearch
├── docker-compose.prod.yml            # Production compose file
├── nginx/                             # Nginx config (reverse proxy)
├── infra/                             # Infrastructure scripts
├── scripts/                           # Utility scripts
├── docs/                              # Developer documentation
└── CLAUDE.md                          # Project technical constitution
```

---

## Directory Purposes

**`backend/src/main/java/tj/radolfa/domain/model/`:**
- Purpose: Business entities and value objects — the heart of the system
- Contains: `Cart`, `CartItem`, `Order`, `OrderItem`, `ProductBase`, `Sku`, `ListingVariant`, `User`, `Money`, `LoyaltyProfile`, `LoyaltyTier`, `Discount`, `DiscountType`, `Payment` and their associated enums
- Key files: `Cart.java`, `ProductBase.java`, `Order.java`, `Money.java`
- Rule: Zero imports from Spring, JPA, Jackson, or Lombok

**`backend/src/main/java/tj/radolfa/application/ports/in/`:**
- Purpose: One interface per user-facing operation; controllers call only these
- Key files: Grouped by domain — `cart/`, `order/`, `payment/`, `product/`, `discount/`, `loyalty/`; flat-level for cross-domain use cases
- Naming: `{Verb}{Domain}UseCase.java` (e.g., `CheckoutUseCase`, `CreateProductUseCase`)

**`backend/src/main/java/tj/radolfa/application/ports/out/`:**
- Purpose: Abstractions over external systems; implemented by `infrastructure/persistence/adapter/`, `infrastructure/s3/`, `infrastructure/search/`, etc.
- Naming: `Load{Domain}Port.java` for reads, `Save{Domain}Port.java` for writes, `{Action}Port.java` for operations

**`backend/src/main/java/tj/radolfa/application/services/`:**
- Purpose: Business orchestration — one class per use case
- Naming: `{Verb}{Domain}Service.java` (mirrors the use case name)
- Key file: `CheckoutService.java` (most complex — coordinates 8 ports)

**`backend/src/main/java/tj/radolfa/application/readmodel/`:**
- Purpose: Read-optimized projection DTOs returned by query use cases; contain business logic methods (e.g., `ListingVariantDto.withLoyalty()`)
- Key files: `ListingVariantDto.java`, `SkuDto.java`, `ListingVariantDetailDto.java`, `HomeSectionDto.java`, `CartView.java`

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/`:**
- Purpose: Implements Out-Port interfaces using JPA entities and JDBC
- Key files: `CartRepositoryAdapter.java`, `OrderRepositoryAdapter.java`, `ProductHierarchyAdapter.java`, `ListingReadAdapter.java`

**`backend/src/main/java/tj/radolfa/infrastructure/web/`:**
- Purpose: REST API surface; maps HTTP to use case calls; no business logic
- Key files: `SecurityConfig.java`, `GlobalExceptionHandler.java`, `TierPricingEnricher.java`
- Controllers: `AuthController`, `CartController`, `OrderController`, `PaymentController`, `ListingController`, `ProductManagementController`, `UserController`, `LoyaltyController`, `SearchController`

**`frontend/src/app/(storefront)/`:**
- Purpose: Storefront route group — public-facing pages
- Key files: `page.tsx` (home), `products/[slug]/page.tsx` (product detail), `checkout/page.tsx`, `search/page.tsx`, `categories/[slug]/products/page.tsx`

**`frontend/src/app/(admin)/`:**
- Purpose: Admin panel route group — protected by role
- Key files: `manage/page.tsx` (product management), `manage/products/[slug]/edit/page.tsx`

**`frontend/src/shared/api/`:**
- Purpose: Single Axios instance used across all API calls
- Key file: `shared/api/axios.ts` — configured with `withCredentials: true`, base URL from `NEXT_PUBLIC_API_BASE_URL`, and 401 auto-refresh interceptor

---

## Key File Locations

**Entry Points:**
- `backend/src/main/java/tj/radolfa/RadolfaApplication.java`: Spring Boot main class
- `frontend/src/app/layout.tsx`: Next.js root layout with providers
- `frontend/src/app/(storefront)/page.tsx`: Storefront home page

**Configuration:**
- `backend/src/main/java/tj/radolfa/infrastructure/web/SecurityConfig.java`: RBAC rules, JWT filter chain, CORS
- `frontend/src/shared/api/axios.ts`: Axios instance, 401 interceptor, token refresh
- `docker-compose.yml`: Local dev services (PostgreSQL, Elasticsearch)
- `docker-compose.prod.yml`: Production container definitions

**Core Business Logic:**
- `backend/src/main/java/tj/radolfa/application/services/CheckoutService.java`: Order creation, pricing, stock, loyalty
- `backend/src/main/java/tj/radolfa/application/services/CreateProductService.java`: Product creation flow
- `backend/src/main/java/tj/radolfa/domain/model/Cart.java`: Cart aggregate with state machine
- `backend/src/main/java/tj/radolfa/domain/service/LoyaltyCalculator.java`: Tier discount calculation

**Error Handling:**
- `backend/src/main/java/tj/radolfa/infrastructure/web/GlobalExceptionHandler.java`: Centralized exception → HTTP status mapping

**Database Migrations:**
- `backend/src/main/resources/db/migration/`: Production Flyway scripts (V1, V3, V5, V6)
- `backend/src/main/resources/db/migration-dev/`: Dev seed data

**Testing:**
- `backend/src/test/java/tj/radolfa/domain/`: Pure domain unit tests
- `backend/src/test/java/tj/radolfa/application/services/`: Application service tests (ports mocked)
- `backend/src/test/java/tj/radolfa/infrastructure/web/`: Controller slice tests

---

## Naming Conventions

**Backend Files:**
- Use cases: `{Verb}{Domain}UseCase.java` — e.g., `CheckoutUseCase`, `UpdateProductPriceUseCase`
- Services: `{Verb}{Domain}Service.java` — e.g., `CheckoutService`, `UpdateProductPriceService`
- In-Ports: `{Verb}{Domain}UseCase.java` — same as use case naming
- Out-Ports: `Load{Domain}Port.java`, `Save{Domain}Port.java`, `{Action}Port.java`
- Persistence adapters: `{Domain}RepositoryAdapter.java` or `{Domain}Adapter.java`
- JPA entities: `{Domain}Entity.java`
- MapStruct mappers: `{Domain}Mapper.java`
- Web DTOs: `{Domain}Dto.java` (responses), `{Action}{Domain}RequestDto.java` (requests)

**Backend Packages:**
- snake_case for sub-packages: `ports/in/`, `ports/out/`
- Domain grouping within `ports/in/`: `cart/`, `order/`, `payment/`, `product/`, `discount/`, `loyalty/`

**Frontend Files:**
- React components: PascalCase `.tsx` — e.g., `ProductCard.tsx`, `CartItemRow.tsx`
- Hooks: camelCase `use` prefix — e.g., `useCart.ts`, `useAuth.ts`
- API modules: `index.ts` or `admin.ts` inside `api/` subdirectory
- Type definitions: `types.ts` inside `model/` subdirectory
- Barrel exports: `index.ts` at each FSD slice root

**Frontend Directories:**
- Widgets: PascalCase — `CartDrawer/`, `ProductList/`, `MegaMenu/`
- Features and entities: kebab-case — `product-creation/`, `product-edit/`, `user-management/`

---

## Where to Add New Code

**New Backend Use Case:**
1. Define interface in `backend/src/main/java/tj/radolfa/application/ports/in/{domain}/New{Verb}{Domain}UseCase.java`
2. Add any new out-ports in `backend/src/main/java/tj/radolfa/application/ports/out/`
3. Implement service in `backend/src/main/java/tj/radolfa/application/services/New{Verb}{Domain}Service.java`
4. Expose via controller method in `backend/src/main/java/tj/radolfa/infrastructure/web/{Domain}Controller.java`
5. Add request/response DTO in `backend/src/main/java/tj/radolfa/infrastructure/web/dto/`
6. Add authorization rule in `SecurityConfig.java` if needed
7. Test in `backend/src/test/java/tj/radolfa/application/services/`

**New Domain Model:**
- Implementation: `backend/src/main/java/tj/radolfa/domain/model/New{Domain}.java`
- JPA entity: `backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/New{Domain}Entity.java`
- MapStruct mapper: `backend/src/main/java/tj/radolfa/infrastructure/persistence/mappers/New{Domain}Mapper.java`
- Spring Data repo: `backend/src/main/java/tj/radolfa/infrastructure/persistence/repository/New{Domain}Repository.java`
- Flyway migration: `backend/src/main/resources/db/migration/V{next}__add_{domain}_table.sql`

**New Frontend Feature:**
- Feature slice: `frontend/src/features/{feature-name}/`
  - `ui/`: React components
  - `api/`: API call functions using `apiClient` from `@/shared/api`
  - `model/`: Types and hooks
  - `index.ts`: Barrel export
- Tests: Co-locate or place in same slice

**New Frontend Entity:**
- Entity slice: `frontend/src/entities/{entity-name}/`
  - `api/index.ts`: TanStack Query `useQuery` hooks
  - `model/types.ts`: TypeScript types
  - `ui/`: Read-only presentation components
  - `index.ts`: Barrel export

**New Widget:**
- Widget slice: `frontend/src/widgets/{WidgetName}/`
  - `ui/`: Main component file(s)
  - `index.ts`: Barrel export
- Import only from `features/`, `entities/`, `shared/`; never from other widgets or `app/`

**Shared Utilities:**
- Generic helpers: `frontend/src/shared/lib/`
- Shared UI components: `frontend/src/shared/components/`
- Shadcn UI components: `frontend/src/shared/ui/`

---

## Special Directories

**`backend/src/main/java/tj/radolfa/application/readmodel/`:**
- Purpose: Read-side projection DTOs optimized for specific query responses
- Generated: No — hand-authored `record` types
- Note: These are NOT domain models; they are output shapes for queries. They live in `application/` because they define the application's query contract, not in `infrastructure/web/dto/` which is HTTP-specific

**`.planning/codebase/`:**
- Purpose: GSD tooling analysis documents consumed by plan/execute agents
- Generated: Yes — by `gsd:map-codebase`
- Committed: Yes

**`backend/target/`:**
- Purpose: Maven build output
- Generated: Yes
- Committed: No

**`frontend/.next/`:**
- Purpose: Next.js build output
- Generated: Yes
- Committed: No

**`infra/`:**
- Purpose: Infrastructure as code, server setup scripts
- Generated: No
- Committed: Yes

**`nginx/`:**
- Purpose: Nginx reverse proxy configuration for production
- Generated: No
- Committed: Yes

---

*Structure analysis: 2026-03-21*
