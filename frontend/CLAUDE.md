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

The backend sends a full pricing block per listing variant and per SKU.
All monetary values are numbers (not strings). Percentages are whole integers (20 = 20%).

**Variant-level fields (always present on `ListingVariant`):**

| Field | Type | Meaning |
|-------|------|---------|
| `originalPrice` | `number` | Pre-discount price of cheapest SKU. Show as strikethrough when a cheaper price exists. |
| `discountPrice` | `number \| null` | Sale price. `null` if no active sale on this variant. |
| `discountPercentage` | `number \| null` | Sale discount %. Whole number. `null` if no sale. |
| `discountName` | `string \| null` | Sale campaign name, e.g. "Winter Collection". |
| `discountColorHex` | `string \| null` | Badge background color for the sale campaign. |
| `loyaltyPrice` | `number \| null` | User's best price (best-of sale vs tier). `null` for guests and no-tier users. |
| `loyaltyPercentage` | `number \| null` | User's tier %. Badge shows this, NOT the effective applied %. |
| `isPartialDiscount` | `boolean` | `true` if only some sizes are on sale or have different campaigns. |

**SKU-level fields (on each `Sku` in `skus[]`):**

Same set as variant-level except `isPartialDiscount` (not on SKU).
When the user selects a size, swap to that SKU's pricing — discount may appear
or disappear depending on whether that SKU is part of an active campaign.

**Display logic:**

| User type | Hero price | Strikethrough |
|-----------|-----------|---------------|
| Guest / no tier, no sale | `originalPrice` (violet) | — |
| Guest / no tier, sale active | `discountPrice` (red) | `originalPrice` |
| Auth + tier, no sale | `loyaltyPrice` (amber, Crown) | `originalPrice` |
| Auth + tier, sale active | `loyaltyPrice` (amber, Crown) | `originalPrice` + sale badge |

**Formula (for reference — computed on backend, do not recompute on frontend):**
`loyaltyPrice = originalPrice × (1 − max(discountPercentage, loyaltyPercentage) / 100)`

### Key Type Field Names
These differ from legacy ERP-era names — use the correct ones:

| Entity | Old (deleted) | Current |
|---|---|---|
| `Sku` | `id`, `erpItemCode`, `price` | `skuId`, `skuCode`, `originalPrice` (+ full pricing block — see Price Model) |
| `ListingVariant` | `id`, `colorHexCode`, `category`, `totalStock`, `minPrice`, `maxPrice`, `tierDiscountedMinPrice` | `variantId`, `colorHex`, `categoryName`, `originalPrice` (+ full pricing block — see Price Model) |
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
| Rating summary | `["rating", slug]` |
| Reviews page | `["reviews", slug, page, sort]` |
| Tags (public list) | `["tags"]` |

---

## Integration Plan

Active migration tracked in `/FRONTEND_INTEGRATION_PLAN.md` at the repo root.
Before implementing any new feature, check if it is already scoped to a phase there.
Mark phases complete with a short summary when done.
