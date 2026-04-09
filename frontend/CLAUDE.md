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

| Entity | Field names |
|---|---|
| `Sku` | `skuId`, `skuCode`, `originalPrice` (+ full pricing block — see Price Model) |
| `ListingVariant` | `variantId`, `colorHex`, `categoryName`, `originalPrice` (+ full pricing block — see Price Model) |
| `AuthResponse` | `accessToken` |
| Pagination | `content`, `last` |
| `HomeSection` items array | `listings` |

---

## Role Model

Three roles exist: `USER`, `MANAGER`, `ADMIN`.

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
- Use "Catalog Data" or "System-managed" labels for read-only sections.

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
| Admin product card | `["admin-product", productBaseId]` |

---

## Integration Plan

Active migration tracked in `/FRONTEND_INTEGRATION_PLAN.md` at the repo root.
Before implementing any new feature, check if it is already scoped to a phase there.
Mark phases complete with a short summary when done.

---

## Design Standards (CRITICAL — read before writing any UI code)

Quality bar: **Wildberries-level professional marketplace**. Every screen must look intentional. Rules below are non-negotiable — no one-off colors, no inline style exceptions, no approximations.

---

### Brand & Color System

**Primary color: Wildberries magenta-purple (`#CB11AB`)**

This is set in `globals.css`:
```css
--primary: 308 84% 43%;
--ring:    308 84% 43%;
```

**Always use semantic tokens — never raw Tailwind color classes for brand color:**
- `bg-primary`, `text-primary`, `border-primary` ← correct
- `bg-fuchsia-700`, `bg-purple-600` ← forbidden (breaks theming)

**Fixed semantic palette:**

| Token | Tailwind / class | Use case |
|---|---|---|
| Primary | `bg-primary` / `text-primary` | CTAs, active nav item, key badges |
| Destructive | `text-rose-600` / `bg-rose-600` | Delete, error, out-of-stock |
| Success | `text-green-600` / `bg-green-600` | Confirmed, in-stock, approved |
| Warning | `text-orange-500` / `bg-orange-500` | Low stock, pending |
| Info | `text-blue-600` / `bg-blue-600` | Informational badges |
| Sale price | `text-rose-600` | Strikethrough original, discount badge |
| Loyalty price | `text-amber-500` | Crown icon + tier price |
| Muted text | `text-muted-foreground` | Labels, captions, secondary info |

**Background layers:**
| Layer | Class |
|---|---|
| Page | `bg-background` (white) |
| Card / panel | `bg-card` (white) |
| Subtle section | `bg-muted/40` or `bg-slate-50` |
| Admin sidebar | `bg-slate-900 text-slate-100` |

---

### Typography

Font: **Geist Sans** — already configured in `layout.tsx`. Do not add other fonts.

| Role | Classes |
|---|---|
| Storefront page title | `text-3xl font-bold tracking-tight` |
| Admin page title | `text-2xl font-semibold` |
| Section heading | `text-sm font-semibold` + `border-b pb-2 mb-4` (with an icon) |
| Card title | `text-base font-semibold` |
| Body text (admin) | `text-sm` |
| Body text (storefront) | `text-base` |
| Caption / label | `text-xs text-muted-foreground` |
| Hero price | `text-xl font-bold tabular-nums` |
| Stat / metric | `text-2xl font-bold tabular-nums` |

---

### Layout

**Storefront:**
- Max content width: `max-w-[1400px] mx-auto px-4` (Wildberries-width, ~1400px)
- Product grid: `grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6`
- Section vertical rhythm: `py-8` between sections

**Admin panel:**
- Structure: fixed left sidebar (240px) + scrollable main content area
- Sidebar: `w-60 bg-slate-900 text-slate-100 flex flex-col h-screen sticky top-0`
- Main area: `flex-1 overflow-y-auto bg-background p-6`
- Content max-width: none — fills the remaining space
- Page header row: `flex items-center justify-between mb-6`
  - Left: page title (`text-2xl font-semibold`)
  - Right: primary action button (e.g. `+ New Product`)

**Standard admin page structure (must follow this template):**
```tsx
// 1. Header row
<div className="flex items-center justify-between mb-6">
  <h1 className="text-2xl font-semibold">Page Title</h1>
  <Button><PlusIcon className="h-4 w-4 mr-2" />Primary Action</Button>
</div>

// 2. Stats row (always present on list/dashboard pages)
<div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
  <StatCard />
</div>

// 3. Main content
<Card>
  <CardContent className="p-0">
    {/* table, list, or form */}
  </CardContent>
</Card>
```

---

### Buttons

Use Shadcn `<Button>` variants only. No custom button styles ever.

| Purpose | `variant` | `size` | Example label |
|---|---|---|---|
| Primary CTA | `default` | `default` | "Save", "Add to cart", "+ New" |
| Secondary / cancel | `outline` | `default` | "Cancel", "Back" |
| Destructive | `destructive` | `default` | "Delete", "Remove" |
| Table row action | `ghost` | `sm` | Edit / Delete icons |
| Inline nav / link | `link` | — | Breadcrumbs |

Rules:
- `size="sm"` in admin tables and tight controls only. Never `size="lg"` in admin.
- Icon + label: `<Icon className="h-4 w-4 mr-2" />Label`
- Icon-only buttons: always wrap in Shadcn `<Tooltip>` — no bare icon buttons.

---

### Cards & Panels

| Context | Classes |
|---|---|
| Admin stat card | `rounded-xl border bg-card p-5 shadow-sm` |
| Admin content panel | `<Card>` (Shadcn — `rounded-xl border bg-card`) |
| Form section block | `bg-muted/30 rounded-xl p-5 space-y-4` |
| Storefront product card | `rounded-xl overflow-hidden shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-150` |

- Card padding: always `p-5` or `p-6`. Never `p-3` or `p-4` for main content.
- Border radius: `rounded-xl` cards, `rounded-lg` inputs/buttons, `rounded-full` badges/pills.

---

### Admin Sidebar

```tsx
<aside className="w-60 bg-slate-900 text-slate-100 flex flex-col h-screen sticky top-0">
  {/* Logo at top */}
  <nav className="flex-1 py-4 space-y-1 px-2">
    {/* Group label */}
    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider px-3 mb-1">
      Group Name
    </p>
    {/* Nav item — default */}
    <a className="flex items-center text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg px-3 py-2 text-sm">
      <Icon className="h-4 w-4 mr-3" />Label
    </a>
    {/* Nav item — active */}
    <a className="flex items-center bg-primary/20 text-primary font-medium rounded-lg px-3 py-2 text-sm">
      <Icon className="h-4 w-4 mr-3" />Label
    </a>
  </nav>
  {/* User footer at bottom: avatar + role */}
</aside>
```

---

### Admin Stat Card Template

```tsx
<div className="rounded-xl border bg-card p-5 shadow-sm">
  <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Label</p>
  <p className="text-2xl font-bold tabular-nums mt-1">Value</p>
  <p className="text-xs text-muted-foreground mt-1">Trend or sub-label</p>
</div>
```

---

### Forms & Inputs

- Field gaps: `space-y-5` inside a form; `space-y-4` inside a section block.
- Always use Shadcn `<Input>` — never a plain `<input>`.
- Always use Shadcn `<Select>` — never a plain `<select>`.
- Color hex inputs: always show a live circular or square color swatch next to the field.
- Numeric range fields (e.g. discount %): slider + number input side-by-side, not a lone number input.
- `input[type="number"]` spinners removed globally — if +/− is needed, use explicit `+`/`−` icon buttons.

**Dialog sizes by content:**

| Content | `className` on `DialogContent` |
|---|---|
| Confirmation (1–2 lines) | `max-w-sm` |
| Simple form (2–4 fields) | `max-w-2xl` |
| Rich form (5+ fields) | `max-w-3xl` or `max-w-4xl` |
| Wizard / product picker | `max-w-4xl` or `max-w-5xl` |

- Never leave >20% of a dialog blank — size up if content is sparse.
- Modal overlay: `bg-black/30` — never the Shadcn default `bg-black/80`.

---

### Feedback & States

| State | Rule |
|---|---|
| Loading | `<Skeleton>` — never a blank area, never a lone spinner |
| Empty state | Centered, `border border-dashed rounded-xl p-12`, icon `h-10 w-10 text-muted-foreground/40`, 1-line message, optional CTA |
| Field error | `text-destructive text-sm` inline under the field |
| API error | `toast.error(getErrorMessage(err))` |
| Success mutation | `toast.success("...")` |
| Destructive confirm | Dialog **must name the specific item** — never a generic "Are you sure?" |
| Status badge | Colored dot **and** colored text — never one alone |

---

### Interactive Patterns

- Interactive cards: `hover:shadow-md hover:-translate-y-0.5 transition-all duration-150`
- Table row action buttons: `variant="ghost" size="sm"` + `<Tooltip>`
- Sortable column headers: `cursor-pointer hover:bg-muted/50 select-none`
- Pagination: always show total — `Showing 1–20 of 143`
