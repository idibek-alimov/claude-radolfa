# Radolfa Frontend — Technical Constitution

## Stack
- Next.js 15 (App Router), React 19, TypeScript strict mode
- TanStack Query v5 — server state. React context / `useState` — client state. No Redux/Zustand.
- Tailwind CSS + Shadcn UI (`shared/ui/`). Axios at `shared/api/axios.ts` (`withCredentials: true`).

---

## Architecture: Feature-Sliced Design (FSD)

```
app → pages → widgets → features → entities → shared
```

Cross-slice imports at the same layer are **forbidden**. Logic (hooks, mutations) lives in `features/` or `entities/`, never in `app/`. `"use client"` only for browser APIs / event handlers / hooks — prefer Server Components for layout wrappers.

---

## Backend API Contract

- Base URL: `/api/v1`. Auth: HTTP-only cookie `authToken` — interceptor handles 401 → refresh → retry.
- Endpoints reference: `reports/02_endpoints_reference.md`.
- Error shape: `{ status, error, message, path }` — read `err.response.data.message` via `getErrorMessage()`.

### Search / Sort / Filter — Never On The Client

**Rule:** Never call `.filter()`, `.sort()`, or `.slice()` on a paginated `content[]`. Pass `search`, `sort`, `page`, `size` as query params; the backend returns the filtered page. Debounce text inputs 300 ms; reset `page` to 1 on `search`/`sort` change. No exemption for "small" lists.

### Pagination
Backend and frontend are both **1-based**. Send `page` as-is. Response: `content[]`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`.

### Price Model

**Variant / SKU fields (use as-is — never recompute on frontend):**

| Field | Type | Meaning |
|---|---|---|
| `originalPrice` | `number` | Pre-discount price. Show as strikethrough when cheaper price exists. |
| `discountPrice` | `number\|null` | Sale price. |
| `discountPercentage` | `number\|null` | Sale %. |
| `discountName` / `discountColorHex` | `string\|null` | Campaign name / badge color. |
| `loyaltyPrice` | `number\|null` | User's best price (sale vs tier). Null for guests. |
| `loyaltyPercentage` | `number\|null` | User's tier %. Show on badge. |
| `isPartialDiscount` | `boolean` | Variant-level only. True if only some SKUs are on sale. |

**Display logic:**

| User type | Hero price | Strikethrough |
|---|---|---|
| Guest / no tier, no sale | `originalPrice` (violet) | — |
| Guest / no tier, sale | `discountPrice` (red) | `originalPrice` |
| Auth + tier, no sale | `loyaltyPrice` (amber, Crown) | `originalPrice` |
| Auth + tier, sale | `loyaltyPrice` (amber, Crown) | `originalPrice` + sale badge |

When user selects a size, swap to that SKU's pricing block.

### Key Type Fields

| Entity | Fields |
|---|---|
| `Sku` | `skuId`, `skuCode`, `originalPrice` (+ full pricing block) |
| `ListingVariant` | `variantId`, `colorHex`, `categoryName`, `originalPrice` (+ full pricing block) |
| `AuthResponse` | `accessToken` |
| Pagination | `content`, `last` |
| `HomeSection` items | `listings` |

---

## Role Model

| Action | USER | MANAGER | ADMIN |
|---|---|---|---|
| Browse / cart / checkout | ✅ | ✅ | ✅ |
| Edit listing enrichment, images, categories (create) | — | ✅ | ✅ |
| **Edit SKU price / stock** | — | ❌ | ✅ only |
| Manage users / refund / reindex | — | view/status | ✅ full |

`ProtectedRoute requiredRole="MANAGER"` must accept both `MANAGER` and `ADMIN`.

---

## Component Rules

- Functional components only. No `useEffect` for data fetching — use TanStack Query.
- Absolute imports: `@/entities/product`, `@/shared/ui/button`, etc.
- Shadcn components: import from `shared/ui/`, not `@/components`.

## Admin Panel Rules

- Price / stock fields: **read-only for MANAGER**. Show `Lock` icon — no disabled inputs that look editable.
- Labels: "Catalog Data" or "System-managed" for read-only sections.

---

## Key Query Keys (TanStack Query)

| Data | Query key |
|---|---|
| Listing grid | `["listings", page, search]` |
| Single listing | `["listing", slug]` |
| Home collections | `["home-collections"]` |
| Category tree / Colors / Tags | `["categories"]` / `["colors"]` / `["tags"]` |
| Cart / My orders | `["cart"]` / `["my-orders"]` |
| Loyalty tiers | `["loyalty-tiers"]` |
| Users (admin) | `["users", page, search]` |
| Reviews / Rating | `["reviews", slug, page, sort]` / `["rating", slug]` |
| Admin product card | `["admin-product", productBaseId]` |

---

## Integration Plan

Active migration tracked in `/FRONTEND_INTEGRATION_PLAN.md`. Check it before implementing any new feature and mark phases complete when done.

---

## Design Standards

Quality bar: **Wildberries-level professional marketplace**. Rules below are non-negotiable.

### Brand & Color

Primary: `#CB11AB` — set in `globals.css` as `--primary: 308 84% 43%`.  
**Always use semantic tokens — never raw Tailwind color classes for brand color.**

| Token | Class | Use |
|---|---|---|
| Primary | `bg-primary` / `text-primary` | CTAs, active nav, key badges |
| Destructive | `text-rose-600` | Delete, error, out-of-stock |
| Success / Warning / Info | `text-green-600` / `text-orange-500` / `text-blue-600` | Status |
| Sale price / Loyalty price | `text-rose-600` / `text-amber-500` | Pricing |
| Muted | `text-muted-foreground` | Labels, captions |

Backgrounds: page `bg-background`, card `bg-card`, subtle `bg-muted/40`, admin sidebar `bg-slate-900`.

### Typography (Geist Sans — do not add other fonts)

| Role | Classes |
|---|---|
| Storefront page title | `text-3xl font-bold tracking-tight` |
| Admin page title / section heading | `text-2xl font-semibold` / `text-sm font-semibold border-b pb-2 mb-4` |
| Hero price / stat | `text-xl font-bold tabular-nums` / `text-2xl font-bold tabular-nums` |
| Body admin / storefront | `text-sm` / `text-base` |
| Caption | `text-xs text-muted-foreground` |

### Layout

**Storefront:** `max-w-[1400px] mx-auto px-4`. Grid: `grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6`.  
**Admin:** fixed `w-60` sidebar (`bg-slate-900`) + scrollable main (`flex-1 overflow-y-auto p-6`). Standard page structure: header row (title + primary action) → stats row (`grid-cols-2 lg:grid-cols-4 gap-4`) → `<Card>` content.

### Buttons (Shadcn `<Button>` only — no custom styles)

| Purpose | `variant` | `size` |
|---|---|---|
| Primary CTA | `default` | `default` |
| Secondary / cancel | `outline` | `default` |
| Destructive | `destructive` | `default` |
| Table row action | `ghost` | `sm` |

Icon-only buttons: always wrap in `<Tooltip>`.

### Cards & Panels

| Context | Classes |
|---|---|
| Admin stat card | `rounded-xl border bg-card p-5 shadow-sm` |
| Admin content panel | Shadcn `<Card>` |
| Form section block | `bg-muted/30 rounded-xl p-5 space-y-4` |
| Storefront product card | `rounded-xl overflow-hidden shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all` |

Padding: `p-5` or `p-6` — never `p-3`/`p-4`. Radii: `rounded-xl` cards, `rounded-lg` inputs, `rounded-full` badges.

### Forms & Inputs

- `space-y-5` inside a form; `space-y-4` inside a section block.
- Always use Shadcn `<Input>` / `<Select>`. Color hex: show live swatch. Numeric range: slider + number input.
- Dialog sizes: confirmation `max-w-sm`, simple form `max-w-2xl`, rich form `max-w-3xl/4xl`, wizard `max-w-4xl/5xl`.
- Modal overlay: `bg-black/30` — not the Shadcn default `bg-black/80`.

### Feedback & States

| State | Rule |
|---|---|
| Loading | `<Skeleton>` — never blank area or lone spinner |
| Empty state | `border border-dashed rounded-xl p-12`, icon `h-10 w-10 text-muted-foreground/40` |
| Field error | `text-destructive text-sm` inline under field |
| API error / Success | `toast.error(getErrorMessage(err))` / `toast.success("...")` |
| Destructive confirm | Dialog must name the specific item — never generic "Are you sure?" |
| Status badge | Colored dot **and** colored text — never one alone |

### Interactive Patterns

- Cards: `hover:shadow-md hover:-translate-y-0.5 transition-all duration-150`
- Table actions: `variant="ghost" size="sm"` + `<Tooltip>`
- Sortable headers: `cursor-pointer hover:bg-muted/50 select-none`
- Pagination: always show total — `Showing 1–20 of 143`

### UI Navigation Pattern Hierarchy (CRITICAL)

**Default: Full Page.** For any new view (detail, form, settings), navigate to a dedicated page (e.g. `/admin/orders/[id]`). Full pages are bookmarkable, deep-linkable, and scale with content.

**Side-Panel Drawers are NOT the default.** Do not reach for `<Sheet>` as the quick solution.

**Drawer is acceptable only for:** supplementary/contextual info the user needs alongside the current view (filter panel, quick-preview), or genuinely short-lived content that does not need its own URL.

**Mandatory:** If you believe a Drawer is the right choice, **stop and ask the user explicitly** before implementing. State why a full page is insufficient.
