# Radolfa Frontend — Technical Constitution

## Stack
- **Framework:** Next.js 15 (App Router) — upgraded from 14 in Phase 0
- **Language:** TypeScript (strict mode — `noImplicitAny: true`)
- **React:** 19 (upgraded from 18 in Phase 0)
- **Server state:** TanStack Query v5 (`useQuery`, `useMutation`, `useQueryClient`)
- **Client state:** React context / `useState` — no Redux, no Zustand unless justified
- **Styling:** Tailwind CSS + Shadcn UI components (`shared/ui/`)
- **HTTP client:** Axios instance at `shared/api/axios.ts` (`withCredentials: true`)

---

## Architecture: Feature-Sliced Design (FSD)

```
app/        — Routing only (page.tsx, layout.tsx). No logic, no imports from features.
pages/      — Page-level composition. Imports from widgets only.
widgets/    — Complex UI blocks (Navbar, CartDrawer, HomeCollections).
features/   — User actions with side effects (auth, cart, checkout, payment).
entities/   — Business domain types + read-only UI (ProductCard, CartItem, User).
shared/     — UI kit (Shadcn), API client, utils, i18n config.
```

**Dependency direction is strictly downward:**
`app → pages → widgets → features → entities → shared`

Cross-slice imports at the same layer are **forbidden**. Example: `features/cart` must not
import from `features/checkout`. If shared logic is needed, extract it to `entities/` or `shared/`.

Logic (hooks, mutations, API calls) lives in `features/` or `entities/`, never in `app/` pages.

---

## Backend API Contract

- **Base URL:** `/api/v1` (proxied via nginx in production; set `NEXT_PUBLIC_API_BASE_URL` for local dev)
- **Auth:** HTTP-only cookie `authToken` (sent automatically via `withCredentials: true`).
  The Axios interceptor in `shared/api/axios.ts` handles 401 → refresh → retry automatically.
- **Reference:** `reports/02_endpoints_reference.md` — authoritative source for all endpoints.
- **Error shape:**
  ```json
  { "status": 400, "error": "Bad Request", "message": "...", "path": "..." }
  ```
  Read `err.response.data.message` for user-facing error toasts via `getErrorMessage()`.

### Pagination
Backend is **1-based**. Frontend page state is also 1-based. Send page as-is (no subtraction):
```ts
params: { page, size }
```
Response fields: `content[]`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`.
The backend now returns all these fields directly — no adapter or transformation needed.

### Price Model
The backend sends three price tiers per listing variant. There are **no** per-variant
sale discounts, sale titles, or per-SKU loyalty prices:

| Field | Meaning |
|---|---|
| `minPrice` | Lowest SKU price (always present) |
| `maxPrice` | Highest SKU price (may equal minPrice) |
| `tierDiscountedMinPrice` | Authenticated user's tier-discounted price (null if no tier) |

Display logic:
- **With tier:** `tierDiscountedMinPrice` as hero price (amber, Crown icon); `minPrice` struck through
- **No tier / guest:** `minPrice` as hero; show range `minPrice – maxPrice` if they differ

Never render `discountedPrice`, `loyaltyPrice`, `saleTitle`, or `saleColorHex` — those fields
were removed from the backend and do not exist.

### Key Type Field Names
These differ from legacy ERP-era names — use the correct ones:

| Entity | Old (deleted) | Current |
|---|---|---|
| `Sku` | `id`, `erpItemCode`, `originalPrice` | `skuId`, `skuCode`, `price` |
| `ListingVariant` | `id`, `colorHexCode`, `category`, `totalStock` | `variantId`, `colorHex`, `categoryName`, *(compute from skus)* |
| `AuthResponse` | `token` | `accessToken` |
| Pagination | `items`, `hasMore` | `content`, `last` |
| `HomeSection` items array | `items` | `listings` |

---

## Role Model

Three roles exist: `USER`, `MANAGER`, `ADMIN`. The old `SYSTEM` role was deleted.

| Action | USER | MANAGER | ADMIN |
|---|---|---|---|
| Browse, cart, checkout, view profile | ✅ | ✅ | ✅ |
| Edit listing enrichment (description, flags) | — | ✅ | ✅ |
| Upload / delete listing images | — | ✅ | ✅ |
| Create product | — | ✅ | ✅ |
| Manage categories | — | ✅ (create) | ✅ (create + delete) |
| Edit color display name / hex | — | ✅ | ✅ |
| **Edit SKU price** | — | ❌ | ✅ only |
| **Edit SKU stock** | — | ❌ | ✅ only |
| Manage users (role, status) | — | view/status | ✅ full |
| Search reindex | — | — | ✅ |
| Refund payment | — | — | ✅ |

`ProtectedRoute` with `requiredRole="MANAGER"` must accept both `MANAGER` and `ADMIN`.

---

## Images

- All images are S3 URLs served from `s3.twcstorage.ru`.
- Always use `<Image src={url} unoptimized />` — never process images on the frontend.
- Java resizes uploads (150×150, 400×400, 800×800) and stores in S3.
- Upload via `POST /listings/{slug}/images` with `multipart/form-data`, field name `files`.
- Delete via `DELETE /listings/{slug}/images` with body `{ "imageUrl": "..." }`.

---

## Component Rules

- **Functional components only.** No class components.
- **No `useEffect` for data fetching** — use TanStack Query.
- **Absolute imports only:** `@/entities/product`, `@/shared/ui/button`, etc.
- **Shadcn components live in `shared/ui/`** — import from there, not from `@/components`.
- `"use client"` only when the component uses browser APIs, event handlers, or hooks.
  Prefer Server Components for layout/structural wrappers.

---

## Admin Panel Rules

- Price and stock fields in the product edit dialog are **read-only for MANAGER**.
  Only `ADMIN` role may call `PUT /admin/skus/{id}/price` or `PUT /admin/skus/{id}/stock`.
- Render a `Lock` icon next to read-only fields. Do not use disabled inputs that look editable.
- No UI labels referencing "ERP" — the system is fully standalone. Use "Catalog Data" or
  "System-managed" for read-only sections.

---

## Key Query Keys (TanStack Query)

Keep query keys consistent to ensure correct cache invalidation:

| Data | Query key |
|---|---|
| Listing grid | `["listings", page, search]` |
| Single listing | `["listing", slug]` |
| Home collections | `["home-collections"]` |
| Category tree | `["categories"]` |
| Cart | `["cart"]` |
| My orders | `["my-orders"]` |
| Loyalty tiers | `["loyalty-tiers"]` |
| Colors | `["colors"]` |
| Users (admin) | `["users", page, search]` |

---

## Integration Plan

Active migration tracked in `/FRONTEND_INTEGRATION_PLAN.md` at the repo root.
Before implementing any new feature, check if it is already scoped to a phase there.
Mark phases complete with a short summary when done.
