# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Context

Radolfa is a standalone e-commerce platform. It is the **authoritative source** for all data (products, prices, stock, orders). Deployed on a single VPS via Docker Compose; assets stored on AWS-compatible S3 (Timeweb Cloud).

Sub-level instructions live in `backend/CLAUDE.md` and `frontend/CLAUDE.md`. Read them when working within those directories.

---

## Commands

### Backend (Java 21 + Spring Boot 3.4.4)

```bash
# Start (dev)
./mvnw spring-boot:run -pl backend

# Test all
./mvnw test -pl backend

# Run a single test class
./mvnw test -pl backend -Dtest=CreateProductServiceTest

# Run a single test method
./mvnw test -pl backend -Dtest=CreateProductServiceTest#shouldCreateProductWithVariants
```

### Frontend (Next.js 15 + React 19)

```bash
# Install dependencies
npm install --prefix frontend

# Start dev server (set API base URL)
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 npm run dev --prefix frontend

# Build for production
npm run build --prefix frontend

# Lint
npm run lint --prefix frontend
```

### Infrastructure

```bash
# Start only the dependencies for local dev
docker-compose up -d postgres elasticsearch

# Start full stack (production-like)
docker-compose up -d
```

---

## Architecture

### Backend: Hexagonal Monolith

Three strict layers — domain has zero framework dependencies:

| Package | Rule |
|---|---|
| `tj.radolfa.domain` | Pure Java only. No Spring, JPA, Jackson. Entities, Value Objects, Domain Exceptions. |
| `tj.radolfa.application` | Use case interfaces (`ports.in`), repository interfaces (`ports.out`), business logic services. |
| `tj.radolfa.infrastructure` | Spring, JPA, REST controllers, S3, Elasticsearch, security, payment. All are adapters. |

**Mapping:** MapStruct only for `DTO ↔ Domain ↔ JPA Entity`. No manual mapping or reflection.

**Transactions:** `@Transactional` on application services, not on adapters — except `SaveCartPort` and `SaveProductHierarchyPort` which are called from multiple services.

**Constructor injection only.** No `@Autowired` on fields.

**Testing:** No Mockito. All tests use hand-written in-memory fake adapters implementing the output ports. See `CreateProductServiceTest` for the canonical example.

**Database migrations:** Flyway. Add a new `V{n}__description.sql` under `backend/src/main/resources/db/migration/`. Never apply DDL manually.

### Frontend: Feature-Sliced Design (FSD)

Dependency direction is strictly **downward**:

```
app → pages → widgets → features → entities → shared
```

Cross-slice imports at the same layer are forbidden. If `features/cart` needs something from `features/checkout`, extract the shared logic to `entities/` or `shared/`.

Logic (hooks, TanStack Query calls) lives in `features/` or `entities/`. The `app/` layer is routing only — no logic, no direct feature imports.

`"use client"` only when a component uses browser APIs, event handlers, or hooks. Prefer Server Components for structural wrappers.

---

## Critical Constraints

### Field Ownership — ERP vs. Radolfa

| Fields | Owner | Consequence |
|---|---|---|
| SKU price, stock, size label | ERP / ADMIN | Overwritten on sync. Never editable by MANAGER. Show `Lock` icon in UI. |
| Product descriptions, images, attributes, tags, dimensions | Radolfa | Never overwritten. MANAGER and ADMIN may edit. |

`FieldLockException` is thrown if locked fields are modified via the wrong path.

### Price Model — Never Recompute on Frontend

The backend sends a full pricing block on every `ListingVariant` and `Sku`. Use these fields as-is; do not recalculate discounts or loyalty prices on the frontend. See `frontend/CLAUDE.md` for the full price display logic table.

### Images

All resizing happens in Java (Thumbnailator → S3 at three sizes: 150×150, 400×400, 800×800). Always render with `<Image src={url} unoptimized />`. No image processing on the frontend.

### Roles

Three roles: `USER`, `MANAGER`, `ADMIN`. `ADMIN` is the only role that can edit SKU price or stock. A `ProtectedRoute` with `requiredRole="MANAGER"` must accept both `MANAGER` and `ADMIN`.

---

## Non-Obvious Architecture Decisions

- **Elasticsearch reindex at startup** — `RadolfaApplication` triggers a reindex on boot as a temporary workaround. It is also re-triggered on product create/update.
- **Token refresh via Axios interceptor** — `shared/api/axios.ts` intercepts 401 responses, calls `/auth/refresh`, then retries the original request. No manual refresh logic needed anywhere else.
- **Pagination is 1-based end-to-end** — both backend and frontend. Send `page` as-is, no offset subtraction.
- **OTP auth** — Login is phone-number + OTP. SMS-based. Rate-limited and configurable.
- **Loyalty tier re-evaluation** — `MonthlyTierEvaluationService` re-evaluates tiers based on monthly spend. Users can opt out via `ToggleLoyaltyPermanentService`.
- **Review/Question moderation** — Both follow PENDING → APPROVED/REJECTED workflow. Votes (helpful/unhelpful) are tracked separately.
- **No "ERP" labels in UI** — The system is fully standalone. Use "Catalog Data" or "System-managed" for read-only sections.
- **Active feature rollout** is tracked in `FRONTEND_INTEGRATION_PLAN.md`. Check it before implementing a new feature.
