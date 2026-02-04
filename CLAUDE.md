# Radolfa Project — Technical Constitution

## Project Context
- **Type:** Monolithic E-commerce Mirror.
- **Core Truth:** ERPNext is the SOURCE OF TRUTH for `price`, `name`, `stock`.
- **Enrichment:** Radolfa App strictly *enriches* data (images, descriptions).
- **Infrastructure:** Single VPS (Docker Compose), AWS S3 for Assets.

## Architecture Guidelines (Strict Enforcement)

### 1. Backend: Hexagonal Monolith
**Package Structure:**
- `tj.radolfa.domain`: **PURE JAVA ONLY.** No Spring, No JPA, No Jackson.
  - Contains: Entities, Value Objects, Domain Exceptions.
- `tj.radolfa.application`:
  - `ports.in`: Use Case Interfaces (e.g., `UpdateProductUseCase`).
  - `ports.out`: Repository Interfaces (e.g., `LoadProductPort`).
  - `services`: Business Logic implementing In-Ports.
- `tj.radolfa.infrastructure`:
  - `web`: REST Controllers (Adapters).
  - `persistence`: JPA Entities & Repositories (Adapters).
  - `erp`: Feign/RestClient for ERPNext sync.

**Rules:**
- **Mapping:** Use MapStruct for `DTO` ↔ `Domain` ↔ `Entity`.
- **Protection:** Never expose `ProductEntity` (JPA) in the Controller API.
- **Safety:** The `enrichWithErpData()` method must exist in Domain to safely merge ERP updates.

### 2. Frontend: Feature-Sliced Design (FSD)
**Structure:**
- `app/`: Routing layer only (`page.tsx`, `layout.tsx`).
- `pages/`: Page composition (imports from Widgets).
- `widgets/`: Complex UI blocks (ProductList, Navbar).
- `features/`: User actions (AddToCart, Search).
- `entities/`: Business domain UI (ProductCard, PriceTag).
- `shared/`: UI Kit (Shadcn), API, Utils.

**Rules:**
- **Direction:** Dependencies must flow downwards (`Pages` -> `Features` -> `Entities`).
- **Imports:** Absolute imports only (`@/entities/product`).
- **Logic:** Server State = TanStack Query. Client State = Context/Zustand.

## Project Commands
- **Backend Start:** `./mvnw spring-boot:run`
- **Backend Test:** `./mvnw test` (Use `-Dtest=ErpSyncTest` for sync logic)
- **Frontend Start:** `npm run dev --prefix frontend`
- **Frontend Build:** `npm run build --prefix frontend`
- **Docker Dev:** `docker-compose up -d postgres elasticsearch`

## Critical Constraints
- **Images:** NO processing on Frontend. NO processing on Fly. Java resizes -> S3 -> Frontend reads S3 URL.
- **Security:** `MANAGER` role cannot change Price. `SYSTEM` role handles ERP Sync.
