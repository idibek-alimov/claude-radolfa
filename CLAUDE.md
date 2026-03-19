# Radolfa Project — Technical Constitution

## Project Context
- **Type:** Standalone E-commerce Platform.
- **Core Truth:** Radolfa is the authoritative source for all data (products, prices, stock, orders).
- **Enrichment:** Radolfa manages its own catalog — images, descriptions, pricing, and inventory.
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

**Rules:**
- **Mapping:** Use MapStruct for `DTO` ↔ `Domain` ↔ `Entity`.
- **Protection:** Never expose JPA entities in the Controller API.
- **Domain mutations:** Use named domain methods (e.g., `applyExternalUpdate()`) — never set fields directly from controllers.

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
- **Backend Test:** `./mvnw test`
- **Frontend Start:** `npm run dev --prefix frontend`
- **Frontend Build:** `npm run build --prefix frontend`
- **Docker Dev:** `docker-compose up -d postgres elasticsearch`

## Critical Constraints
- **Images:** NO processing on Frontend. Java resizes -> S3 -> Frontend reads S3 URL.
- **Security:** `MANAGER` role cannot change Price or Stock. `ADMIN` role handles catalog management and all privileged operations. No external sync roles exist.
