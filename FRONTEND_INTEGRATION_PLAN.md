# Frontend Integration Plan

> **Goal:** Bring the frontend in full alignment with the standalone backend (post Phase 10
> migration). The backend no longer mirrors ERPNext — it owns all data. The frontend must
> drop ERP-era assumptions (per-SKU discounts, sale titles, SYSTEM role, SyncResult, etc.)
> and gain the new standalone flows: cart, checkout, payments, and admin product creation.
>
> Each phase leaves the app in a working, deployable state.
> Mark phases as done and add a short summary as they are completed.
>
> Reference: `reports/02_endpoints_reference.md` (backend contract, updated 2026-03-20).

---

## Completion Log

| Phase | Status | Summary |
|-------|--------|---------|
| 0 | [x] | Next.js 15, React 19, ESLint 9 upgraded; Turbopack enabled; async cookies() and useRef initial-value fixes applied; build ✅ |
| 1 | [x] | All type contracts updated (Sku, ListingVariant, roles, auth, pagination); API functions migrated to new field names; all UI consumers fixed; tsc ✅ build ✅ |
| 2 | [x] | All product display components already updated during Phase 1; zero stale field references confirmed by grep; tsc ✅ |
| 3 | [x] | Switched useAuth to /users/me; removed stale listings invalidation; ADMIN role badge fixed in profile; recentEarnings section added to LoyaltyDashboard; tsc ✅ build ✅ |
| 4 | [x] | Created entities/cart (types + API), features/cart (hooks + CartItemRow), widgets/CartDrawer (Sheet + CartIconButton); Navbar updated with cart icon badge; ProductDetail updated with quantity stepper + Add to Cart button; tsc ✅ build ✅ |
| 5 | [x] | Created features/checkout (API + types), pages/checkout/CheckoutPage with cart summary + loyalty points + notes + total breakdown; added cancelOrder to profile API; Cancel button on PENDING orders in profile; Order.status typed union; tsc ✅ build ✅ |
| 6 | [x] | Created features/payment (initiatePayment + getPaymentStatus); views/payment/return/PaymentReturnPage with 30s polling, completed/pending/refunded/error states; updated CheckoutPage to call initiatePayment after checkout → redirect to provider; renamed FSD pages/ layer to views/ (avoids Next.js Pages Router conflict); tsc ✅ build ✅ |
| 7 | [ ] | |
| 8 | [ ] | |

---

## Phase 0 — Dependency Upgrades

**Goal:** Upgrade the framework and tooling to current stable versions before any feature
work begins. Doing this first means all subsequent phases are built on the final stack,
not a version that will be upgraded mid-flight.

**Do not mix this phase with Phase 1 feature work.** Get a clean build and deploy first.

### 0.1 Next.js 14 → 15 + React 18 → 19

These two are upgraded together — Next.js 15 is built for React 19.

**`package.json` changes:**
```json
"next": "^15",
"react": "^19",
"react-dom": "^19",
"eslint-config-next": "^15"
```
```json
"@types/react": "^19",
"@types/react-dom": "^19"
```

**Breaking changes to fix immediately after upgrading:**

**1. `params` and `searchParams` in page components are now Promises.**

Every `page.tsx` that reads `params` must be updated:

```ts
// Before (Next.js 14)
export default function Page({ params }: { params: { slug: string } }) {
  return <Component slug={params.slug} />;
}

// After (Next.js 15)
export default async function Page({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  return <Component slug={slug} />;
}
```

Affected files:
- `app/(storefront)/products/[slug]/page.tsx` — reads `params.slug`
- `app/(storefront)/categories/[slug]/products/page.tsx` — reads `params.slug`
- `app/(storefront)/collections/[key]/page.tsx` — reads `params.key`

`searchParams` follows the same pattern wherever used (currently `useSearchParams()` hook
is used client-side, so those are unaffected — only server component props change).

**2. `generateMetadata` must also await params:**
```ts
// After (Next.js 15)
export async function generateMetadata(
  { params }: { params: Promise<{ slug: string }> }
): Promise<Metadata> {
  const { slug } = await params;
  // ...
}
```

**3. Default fetch caching is now OFF.**

In Next.js 14, `fetch()` was cached by default (equivalent to `cache: 'force-cache'`).
In Next.js 15, it defaults to `cache: 'no-store'`. This only affects native `fetch()` in
Server Components — the app uses Axios for all data fetching (client-side via TanStack Query),
so **this breaking change does not affect this codebase** in practice.

**What you gain:**
- Turbopack enabled by default in `next dev` — significantly faster HMR
- React 19 foundation: `use()` hook, server Actions, React Compiler readiness
- Active LTS support (Next.js 14 security patches are winding down)

### 0.2 `@types/node` ^20 → ^22

Node.js 22 is the current LTS. One-line change in `devDependencies`:
```json
"@types/node": "^22"
```

No code changes required.

### 0.3 ESLint 8 → 9

Next.js 15 supports ESLint 9. The main change is the config file format.

**`package.json` change:**
```json
"eslint": "^9"
```

**Config migration:** ESLint 9 uses a flat config (`eslint.config.mjs`) instead of
`.eslintrc.json`. `eslint-config-next` v15 supports both formats. The minimal path is to
keep `.eslintrc.json` and add `"ESLINT_USE_FLAT_CONFIG": "false"` to the environment, then
migrate the config file at your own pace. This is a low-risk, low-urgency step.

### 0.4 Tailwind CSS v3 → v4 (out of scope for this plan)

Tailwind v4 is a larger migration — CSS-first config, no `tailwind.config.js`, different
plugin API, some class name changes. It offers ~10× faster builds and a cleaner config model.

**This is deliberately excluded from this plan.** Treat it as a separate, dedicated project
after all 8 integration phases are complete. Mixing a Tailwind v4 migration with the backend
integration work would make debugging significantly harder.

### 0.5 Definition of Done

- `npm run build` exits with zero errors on Next.js 15 + React 19
- `npm run dev` starts with Turbopack (you will see "▲ Next.js 15.x.x (Turbopack)" in the terminal)
- All three dynamic route pages (`[slug]`, `[key]`) render correctly with async params
- No TypeScript errors (`npx tsc --noEmit` clean)
- `CLAUDE.md` stack line updated: `Next.js 15 (App Router)`, `React 19`

---

## Pre-Work: Gap Analysis

### Type Contract Mismatches (breaking)

The following fields exist in the frontend types but are **not returned** by the new backend:

| Frontend type | Stale field(s) | New backend field(s) |
|---|---|---|
| `Sku` | `id`, `erpItemCode` | `skuId`, `skuCode` |
| `Sku` | `originalPrice`, `discountedPrice`, `loyaltyPrice` | `price` (single field) |
| `Sku` | `discountPercentage`, `loyaltyDiscountPercentage`, `saleTitle`, `saleColorHex`, `onSale`, `discountedEndsAt` | *(removed — no per-SKU sale model)* |
| `ListingVariant` | `id` | `variantId` |
| `ListingVariant` | `name` | *(no `name` in grid response — derived from slug or fetched on detail)* |
| `ListingVariant` | `category` | `categoryName` |
| `ListingVariant` | `colorHexCode` | `colorHex` |
| `ListingVariant` | `originalPrice`, `discountedPrice`, `loyaltyPrice` | `minPrice`, `maxPrice`, `tierDiscountedMinPrice` |
| `ListingVariant` | `discountPercentage`, `loyaltyDiscountPercentage`, `saleTitle`, `saleColorHex` | *(removed)* |
| `ListingVariant` | `totalStock` | *(removed — stock is per-SKU)* |
| `ListingVariantDetail` | `siblingVariants[]` | *(removed from API)* |
| `PaginatedListings` | `items`, `hasMore` | `content`, `last` |
| `HomeSection` | `items` | `listings` |
| `CollectionPage` | `{ key, title, page: PaginatedListings }` | `{ title, key, listings[] }` (no pagination wrapper) |
| `AuthResponse` | `token` | `accessToken` |
| `UserRole` | `SYSTEM` | `ADMIN` |
| `LoyaltyProfile` | *(missing)* | `recentEarnings[]` |
| `SyncResult` | entire type | *(deleted — ERP era)* |

### API Function Bugs (silent runtime failures)

| Function | Bug | Fix |
|---|---|---|
| `fetchListings()` | sends `limit` param | backend uses `size` |
| `fetchListings()` | sends 1-based page | backend is 0-based (`page - 1`) |
| `uploadListingImage()` | FormData field named `image` | backend expects field named `files` |
| `removeListingImage()` | body `{ url: imageUrl }` | backend expects `{ imageUrl: imageUrl }` |
| `fetchHomeCollections()` | expects `HomeSection[]` | backend wraps: `{ sections: HomeSection[] }` |

### Missing Entire Features

| Feature | Backend endpoints ready | Frontend |
|---|---|---|
| Cart | `GET/POST/PUT/DELETE /api/v1/cart` | ❌ not built |
| Checkout | `POST /api/v1/orders/checkout` | ❌ not built |
| Payment initiation | `POST /api/v1/payments/initiate/{orderId}` | ❌ not built |
| Payment status poll | `GET /api/v1/payments/{orderId}` | ❌ not built |
| Order cancellation | `PATCH /api/v1/orders/{orderId}/cancel` | ❌ not built |
| Create product | `POST /api/v1/admin/products` | ❌ not built |
| Edit SKU price (ADMIN) | `PUT /api/v1/admin/skus/{id}/price` | ❌ not built |
| Edit SKU stock (ADMIN) | `PUT /api/v1/admin/skus/{id}/stock` | ❌ not built |
| Category management | `POST/DELETE /api/v1/admin/categories` | ❌ not built |
| Color management | `PATCH /api/v1/colors/{id}` | ❌ not built |
| Search reindex | `POST /api/v1/search/reindex` | ❌ not built |
| Loyalty recent earnings display | embedded in `GET /users/me` | ❌ not rendered |

---

## Phase 1 — Foundation: Type Contracts & API Client Fixes

**Goal:** Realign all TypeScript type files and API client functions with the backend contract.
No new UI is added. Pages may temporarily render `—` or `0` where old fields were used,
but the app will not crash or have TypeScript errors. This is the prerequisite for all
subsequent phases.

### 1.1 Delete Stale Types

**`shared/api/types.ts`**
- Delete the entire `SyncResult` interface and its doc comment.
- Repurpose this file to hold the shared `PaginatedResponse<T>` generic interface:
```ts
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;   // 0-based current page
  size: number;
  first: boolean;
  last: boolean;
}
```

### 1.2 Rewrite `entities/product/model/types.ts`

Replace the entire file with types matching `reports/02_endpoints_reference.md` §2:

**`Sku`** (matches `SkuDto`):
```ts
export interface Sku {
  skuId: number;
  skuCode: string;
  sizeLabel: string;
  stockQuantity: number;
  price: number;
}
```

**`ListingVariant`** (matches `ListingVariantDto`):
```ts
export interface ListingVariant {
  variantId: number;
  productCode: string;
  slug: string;
  colorKey: string;
  colorDisplayName: string;
  colorHex: string | null;
  categoryName: string | null;
  topSelling: boolean;
  featured: boolean;
  images: string[];
  minPrice: number;
  maxPrice: number;
  tierDiscountedMinPrice: number | null;
  skus: Sku[];
}
```

**`ListingVariantDetail`** (matches `ListingVariantDetailDto`):
```ts
export interface ListingVariantDetail extends ListingVariant {
  productBaseId: number;
  categoryId: number | null;
  webDescription: string | null;
  attributes: Attribute[];
  // No siblingVariants — removed from API
}
```

**`Attribute`**: unchanged (key, value, sortOrder).

**`PaginatedListings`**: reference `PaginatedResponse<ListingVariant>` from `shared/api/types`.

**`HomeSection`**:
```ts
export interface HomeSection {
  key: string;
  title: string;
  listings: ListingVariant[];  // was "items"
}
```

**`CollectionPage`**:
```ts
export interface CollectionPage {
  key: string;
  title: string;
  listings: ListingVariant[];  // no pagination — returns all items
}
```

**`CategoryTree`**: unchanged.

**`Color`**: rename `hexCode` → `hexCode` (already correct in backend response, verify).

### 1.3 Rewrite `entities/user/model/types.ts`

- Add `ADMIN = "ADMIN"` to `UserRole`, remove `SYSTEM = "SYSTEM"`.
- Remove the ERP comment from the enum docblock.
- Add `recentEarnings` to `LoyaltyProfile` (in `entities/loyalty/model/types.ts`):
```ts
export interface LoyaltyEarning {
  orderId: number;
  pointsEarned: number;
  orderAmount: number;
  orderedAt: string;  // ISO-8601
}

export interface LoyaltyProfile {
  points: number;
  tier: LoyaltyTier | null;
  spendToNextTier: number | null;
  spendToMaintainTier: number | null;
  currentMonthSpending: number | null;
  recentEarnings: LoyaltyEarning[];
}
```

### 1.4 Update `features/auth/model/types.ts`

- Rename `AuthResponse.token` → `AuthResponse.accessToken`.

### 1.5 Fix `entities/product/api/index.ts`

| Function | Change |
|---|---|
| `fetchListings(page, limit)` | rename `limit` → `size`; convert page to 0-based (`page - 1`) |
| `searchListings(q, page, limit)` | same page/size fix |
| `fetchCollectionPage(key, page, limit)` | same page/size fix |
| `fetchCategoryProducts(slug, page, limit)` | same page/size fix |
| `fetchHomeCollections()` | unwrap `{ sections }` from response: `return data.sections` |
| `uploadListingImage()` | rename FormData field: `form.append("files", file)` |
| `removeListingImage()` | fix body: `data: { imageUrl }` (not `{ url }`) |

### 1.6 Fix `features/auth/api/index.ts`

- After `verifyOtp`, the response contains `accessToken` not `token`. Map it:
  `return { ...data, token: data.accessToken }` temporarily, or update `AuthResponse` and
  all consumers simultaneously.

### 1.7 Definition of Done

`npx tsc --noEmit` exits with zero errors. All API function parameters and response
destructuring use the new field names. No `erpItemCode`, `SYSTEM`, `SyncResult`, `originalPrice`
(in types), `discountedPrice`, or `loyaltyPrice` remain in any `.ts` / `.tsx` file.

---

## Phase 2 — Product Display Layer Overhaul

**Goal:** Every product-facing UI component renders correctly with the new data shape.
`ProductCard`, `ProductDetail`, `HomeCollections`, `ProductGrid`, `CatalogSection`,
`TopSellingSection` all use new field names and the new price model.

### 2.1 New Price Model

The backend no longer sends per-listing sale discounts or loyalty prices inline.
Instead it sends three price tiers:
- `minPrice` — lowest SKU price (always present)
- `maxPrice` — highest SKU price (may equal minPrice if all sizes same price)
- `tierDiscountedMinPrice` — the logged-in user's tier-discounted minimum price (null if no tier)

**Price display logic:**

| State | Hero price | Supporting line |
|---|---|---|
| Authenticated with tier | `tierDiscountedMinPrice` (amber, Crown icon) | `minPrice` struck-through |
| No tier or not logged in | `minPrice` in violet | `maxPrice` if different (size range) |

Remove all references to `discountedPrice`, `loyaltyPrice`, `saleTitle`, `saleColorHex`,
`discountPercentage`, `loyaltyDiscountPercentage`, `onSale`. These fields no longer exist.

### 2.2 Update `entities/product/ui/ProductCard.tsx`

- Replace `listing.colorHexCode` → `listing.colorHex`
- Replace `listing.category` → `listing.categoryName`
- Remove `listing.name` fallback (if name is gone from listing grid, derive from slug:
  `slug.split('-').map(capitalize).join(' ')` or leave blank pending backend clarification)
- Replace price section: use `minPrice`/`maxPrice`/`tierDiscountedMinPrice`
- Remove `saleColorHex` border style, sale title badge, discount % badge, loyalty % badge
- Stock: compute from `listing.skus.reduce(sum of stockQuantity)` since `totalStock` removed
- Keep hover image swap, topSelling badge, featured display

### 2.3 Update `entities/product/ui/ProductDetail.tsx`

- Replace `listing.colorHexCode` → `listing.colorHex`
- Replace `listing.category` → `listing.categoryName`
- Remove `siblingVariants` — color swatches section is removed (or simplified to single-color display)
- Update SKU selector: `sku.id` → `sku.skuId`; price from `sku.price` (no originalPrice per SKU)
- Update price block: same new model as ProductCard (minPrice / tierDiscountedMinPrice)
- When a SKU is selected, show `sku.price` as the specific-size price (may differ from minPrice)
- Stock indicator uses `sku.stockQuantity` (when SKU selected) or `listing.skus` aggregate
- Keep attributes, webDescription, productCode display unchanged

### 2.4 Update `widgets/HomeCollections/ui/HomeCollections.tsx`

- API now returns `{ sections: HomeSection[] }` — unwrapping is done in the API client
  (Phase 1), so this component just iterates `sections` as before but uses `section.listings`
  instead of `section.items`.

### 2.5 Update Pagination in All Product Grids

Replace `data.items` → `data.content`, `data.hasMore` → `!data.last`, across:
- `widgets/ProductList/ui/CatalogSection.tsx`
- `widgets/ProductList/ui/TopSellingSection.tsx`
- `widgets/ProductList/ui/ProductGrid.tsx`
- `app/(storefront)/products/page.tsx`
- `app/(storefront)/search/page.tsx`
- `app/(storefront)/categories/[slug]/products/page.tsx`
- `app/(storefront)/collections/[key]/page.tsx`

### 2.6 Update Admin Product Table (`app/(admin)/manage/page.tsx`)

- Replace `item.id` → `item.variantId` in `key` props
- Replace `item.name` (if removed from backend) with slug-based or productCode display
- Replace `item.totalStock` with computed sum from `item.skus`
- Replace price display: use `item.minPrice`
- Rename label "ERP Synced Data" → "Catalog Data"
- Rename "ERP-locked" comments/labels → "Managed by system" or remove the Lock metaphor

### 2.7 Definition of Done

All product grid, detail, and admin product table pages render without errors.
No references to `discountedPrice`, `loyaltyPrice`, `totalStock`, `colorHexCode`,
`saleTitle`, or `siblingVariants` remain in `.tsx` files.

---

## Phase 3 — Auth & User System Cleanup

**Goal:** Auth flow uses `accessToken`, role model reflects `ADMIN`/`MANAGER`/`USER`,
loyalty recent earnings are displayed in the profile, and all `SYSTEM` role references
in the UI are removed.

### 3.1 Update `features/auth/api/index.ts`

- `verifyOtp` returns response where `accessToken` is the JWT field.
  Update all consumers that read `authResponse.token` → `authResponse.accessToken`.

### 3.2 Update `features/auth/model/useAuth.ts`

- `GET /api/v1/auth/me` returns `UserDto` (no loyalty data, per the quick-identity check).
  After login, also call `GET /api/v1/users/me` to get the full profile with loyalty data,
  OR rely solely on `GET /api/v1/users/me` for full user state (preferred — single source).
- Decision: use `/auth/me` only for session check (is logged in?), then enrich via
  `/users/me` for full profile with loyalty + `recentEarnings`.
- Remove the `queryClient.invalidateQueries` for `listings` on auth — tier pricing is
  handled server-side via the `tierDiscountedMinPrice` field (always present or null).

### 3.3 Update `app/(storefront)/profile/page.tsx`

- Replace `ORDER_STEPS = ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED"]`
  → `["PENDING", "PAID", "SHIPPED", "DELIVERED"]` (backend status names changed)
- Replace `avatarRing` logic: remove `user?.role === "SYSTEM"` check; add `ADMIN` styling
- Add `ADMIN` role badge alongside `MANAGER` in the role display chip
- Remove `SYSTEM` from the conditional rendering everywhere in this file

### 3.4 Update `entities/user/model/types.ts`

- Done in Phase 1. Verify `UserRole.ADMIN` is used consistently in the profile page.

### 3.5 Update `widgets/loyalty-dashboard/LoyaltyDashboard.tsx`

- Add a "Recent Earnings" section at the bottom of the dashboard, iterating
  `loyalty.recentEarnings[]`:
  - Per entry: `orderedAt` date, `orderAmount` in TJS, `+ pointsEarned points`
  - Limit to 5 most recent (sorted by `orderedAt` desc)
- `LoyaltyProfile.recentEarnings` is always an array (may be empty), so guard with `if (recentEarnings.length > 0)`.

### 3.6 Update `shared/components/ProtectedRoute.tsx`

- Add `ADMIN` as an acceptable role wherever `MANAGER` is checked.
- Verify `requiredRole="MANAGER"` on the admin manage page includes ADMIN:
  ADMIN is a superset of MANAGER, so ProtectedRoute must accept both.

### 3.7 Definition of Done

Login flow works end-to-end with `accessToken`. Profile page shows `ADMIN` role correctly.
Recent earnings render in the loyalty tab. No `SYSTEM` string exists in any `.tsx` file.

---

## Phase 4 — Cart

**Goal:** Users can add items to cart, view their cart via a slide-out drawer, update
quantities, and remove items. Cart item count badge shows in the Navbar.

### 4.1 New Entity: `entities/cart/`

**`entities/cart/model/types.ts`**:
```ts
export interface CartItem {
  skuId: number;
  skuCode: string;
  productName: string;
  colorName: string;
  sizeLabel: string;
  imageUrl: string | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  availableStock: number;
  inStock: boolean;
}

export interface Cart {
  cartId: number;
  items: CartItem[];
  totalAmount: number;
  itemCount: number;
}
```

**`entities/cart/api/index.ts`**:
```ts
getCart(): Promise<Cart>                            // GET /api/v1/cart
addToCart(skuId, quantity): Promise<Cart>           // POST /api/v1/cart/items
updateCartItem(skuId, quantity): Promise<Cart>      // PUT /api/v1/cart/items/{skuId}
removeCartItem(skuId): Promise<Cart>                // DELETE /api/v1/cart/items/{skuId}
clearCart(): Promise<void>                          // DELETE /api/v1/cart
```

### 4.2 New Feature: `features/cart/`

**`features/cart/hooks/useCart.ts`** — TanStack Query wrapper:
- `useCartQuery()` — fetches cart, caches under `["cart"]`, authenticated only
- `useAddToCart()` — mutation, optimistic update on item count
- `useUpdateCartItem()` — mutation
- `useRemoveCartItem()` — mutation
- `useClearCart()` — mutation
- All mutations invalidate `["cart"]` on success

**`features/cart/ui/CartItemRow.tsx`** — single cart line item:
- Product thumbnail, name, size, color
- Quantity stepper (+/- buttons, input field)
- `inStock: false` → red "Out of stock" warning (item still shown but cannot proceed)
- Unit price × quantity = line total
- Remove button (×)

### 4.3 New Widget: `widgets/CartDrawer/`

**`widgets/CartDrawer/ui/CartDrawer.tsx`** — Shadcn `Sheet` (slide-in from right):
- Opened by clicking the cart icon in the Navbar
- Empty state: "Your cart is empty" with link to products
- Item list: `CartItemRow` per item, scrollable
- Footer: subtotal, "Proceed to Checkout" button → `/checkout`
- "Clear cart" link (subtle, destructive)
- Out-of-stock items highlighted — checkout button disabled if any `inStock: false`

### 4.4 Update `widgets/Navbar/ui/Navbar.tsx`

- Add `ShoppingCart` icon button (from lucide-react) in the right action cluster
- Show `itemCount` badge (red dot with number) when cart has items
- Clicking opens `CartDrawer`
- Cart is fetched only if the user is authenticated (`isAuthenticated` from `useAuth`)

### 4.5 Update `entities/product/ui/ProductDetail.tsx`

- Add **"Add to Cart"** button below the size selector
- Button enabled only when a SKU is selected and it is in stock
- On click: `useAddToCart()` mutation for `selectedSku.skuId, quantity: 1`
- Success: toast "Added to cart" + cart drawer opens
- Error `400`: toast "Out of stock"
- Quantity selector (1–available stock) above the Add button

### 4.6 Definition of Done

Authenticated user can add a SKU to cart from the product detail page. Cart drawer
shows items with correct totals. Quantity update and removal work. Cart count badge
is visible in the Navbar. Unauthenticated users do not see the cart icon (redirect to login
if they try to access `/checkout` directly).

---

## Phase 5 — Checkout & Order Management

**Goal:** Users can convert their cart into an order, optionally redeeming loyalty points.
Existing orders can be cancelled from the profile page.

### 5.1 New API calls in `features/checkout/api.ts`

```ts
checkout(loyaltyPointsToRedeem: number, notes?: string): Promise<CheckoutResponse>
  // POST /api/v1/orders/checkout

cancelOrder(orderId: number): Promise<OrderDto>
  // PATCH /api/v1/orders/{orderId}/cancel
```

**Types**:
```ts
export interface CheckoutRequest {
  loyaltyPointsToRedeem: number;
  notes?: string;
}

export interface CheckoutResponse {
  orderId: number;
  status: string;
  subtotal: number;
  tierDiscount: number;
  pointsDiscount: number;
  total: number;
}
```

### 5.2 New Page: `app/(storefront)/checkout/page.tsx`

- Protected (USER+), renders a `CheckoutPage` component from `pages/checkout/`
- Steps:
  1. **Order summary** — read-only list of cart items (from cart query)
  2. **Loyalty points** — if user has `loyalty.points > 0`: slider or number input for
     points to redeem; show monetary value (`points × 0.01 TJS`)
  3. **Notes** — optional text field
  4. **Total breakdown** — subtotal, tier discount, points discount, **total**
  5. **Place Order** button → calls `checkout()` → navigates to payment initiation

- Display tier discount from `tierDiscountedMinPrice` → actual percentage derived from
  `CheckoutResponse.tierDiscount` (returned server-side, authoritative)
- If cart is empty on mount: redirect to `/products`
- If cart has any `inStock: false` items: show warning, disable checkout

### 5.3 Update Profile Orders Tab

- Add **"Cancel"** button on orders with `status === "PENDING"` (user's own right)
- Cancel triggers `cancelOrder(order.id)` mutation → refetches `["my-orders"]`
- Fix order timeline steps:
  - Replace `"CONFIRMED"` → `"PAID"` in `ORDER_STEPS` array and `STEP_ICONS`/`STEP_KEYS` maps
  - Backend statuses: `PENDING → PAID → SHIPPED → DELIVERED | CANCELLED`

### 5.4 Update `features/profile/types.ts`

- Ensure `Order` type reflects new backend fields:
```ts
export interface OrderItem {
  productName: string;
  quantity: number;
  price: number;
}

export interface Order {
  id: number;
  status: 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  totalAmount: number;
  createdAt: string;
  items: OrderItem[];
}
```

### 5.5 Definition of Done

User can complete the full cart → checkout → order created flow. `PENDING` orders can
be cancelled from the profile. Order timeline shows `PAID` (not `CONFIRMED`).

---

## Phase 6 — Payment Flow

**Goal:** After checkout, the user is redirected to the payment provider. On return,
payment status is polled and the UI confirms success or failure. Loyalty points are
awarded automatically (backend does this — frontend just refreshes the user profile).

### 6.1 New API calls in `features/payment/api.ts`

```ts
initiatePayment(orderId: number): Promise<PaymentInitResponse>
  // POST /api/v1/payments/initiate/{orderId}

getPaymentStatus(orderId: number): Promise<PaymentStatusResponse>
  // GET /api/v1/payments/{orderId}
```

**Types**:
```ts
export interface PaymentInitResponse {
  paymentId: number;
  redirectUrl: string;
}

export interface PaymentStatusResponse {
  paymentId: number;
  status: 'PENDING' | 'COMPLETED' | 'REFUNDED';
  provider: string;
  amount: number;
}
```

### 6.2 Checkout → Payment Bridge

After `checkout()` succeeds (returns `orderId`):
1. Immediately call `initiatePayment(orderId)` to get `redirectUrl`
2. Show a "Redirecting to payment..." loading state
3. `window.location.href = redirectUrl` to send user to provider

### 6.3 New Page: `app/(storefront)/payment/return/page.tsx`

The provider redirects back to `/payment/return?orderId=XX` (configure this URL in the
payment provider's callback settings).

Page logic:
1. Read `orderId` from query params
2. Poll `getPaymentStatus(orderId)` every 2 seconds (up to 30 seconds)
3. On `status === "COMPLETED"`:
   - Show "Payment successful 🎉" with order summary
   - Call `refreshUser()` from `useAuth` (backend has already awarded loyalty points)
   - Clear cart query cache (`queryClient.invalidateQueries(["cart"])`)
   - Link to profile orders tab
4. On `status === "PENDING"` after timeout:
   - Show "Payment is being processed" with manual refresh button
5. On `status === "REFUNDED"` or error:
   - Show failure state, link to contact support

### 6.4 Update `app/(storefront)/checkout/page.tsx`

- Replace "Place Order" flow: after checkout succeeds, automatically call
  `initiatePayment` and redirect rather than showing a static confirmation page.

### 6.5 Definition of Done

Full e-commerce loop works: browse → add to cart → checkout → redirect to payment provider
→ return → status confirmed → loyalty points visible in profile. Cart is cleared on success.

---

## Phase 7 — Admin: Product Creation & Price/Stock Management

**Goal:** Managers can create new products. Admins (not Managers) can update per-SKU
price and stock. This unblocks the full catalog management workflow.

### 7.1 New API calls in `entities/product/api/admin.ts`

```ts
createProduct(payload: CreateProductRequest): Promise<CreateProductResponse>
  // POST /api/v1/admin/products

updateSkuPrice(skuId: number, price: number): Promise<SkuPriceResponse>
  // PUT /api/v1/admin/skus/{skuId}/price

updateSkuStock(skuId: number, payload: UpdateStockPayload): Promise<SkuStockResponse>
  // PUT /api/v1/admin/skus/{skuId}/stock
```

**Types**:
```ts
export interface SkuDefinition {
  sizeLabel: string;
  price: number;
  stockQuantity: number;
}

export interface CreateProductRequest {
  name: string;
  categoryId: number;
  colorId: number;
  skus: SkuDefinition[];
}

export interface CreateProductResponse {
  productBaseId: number;
  variantId: number;
  slug: string;
}

export interface UpdateStockPayload {
  quantity?: number;  // absolute set
  delta?: number;     // relative adjustment (signed)
}
```

### 7.2 Fetch Colors for Product Form

**`entities/color/api.ts`** (new entity slice):
```ts
fetchColors(): Promise<Color[]>   // GET /api/v1/colors
```

### 7.3 New "Create Product" Dialog in Admin Panel

Add a **"+ New Product"** button to the `ProductManagement` component in `manage/page.tsx`.

**`CreateProductDialog`** component (within `features/product-creation/`):
- **Step 1 — Basic Info**: name (text input), category (dropdown from `useQuery(categoryTree)`),
  color (dropdown from `useQuery(colors)` — shows color swatch + display name)
- **Step 2 — SKUs**: dynamic list of size rows, each with sizeLabel, price, stockQuantity inputs
  - "Add size" button, remove per-row
  - Validation: at least one SKU, price ≥ 0, stock ≥ 0
- **Submit**: calls `createProduct()`, on success → toast + close dialog + invalidate `["listings"]`

### 7.4 ADMIN-Only Price & Stock Editing in Product Table

In the existing `ProductManagement` edit dialog (opened per product):

- If `user.role === "ADMIN"`: render editable price and stock fields per SKU
  (replace the read-only `Lock` icon rows in the SKU table)
- Price row: input with `PUT /admin/skus/{skuId}/price` on blur/confirm
- Stock row: two inputs — "Set to" (absolute) or "Adjust by ±" (delta) — one send button
- If `user.role === "MANAGER"`: keep current read-only display (Managers cannot touch price/stock)

### 7.5 Definition of Done

Manager can create a new product with color and SKUs via the admin panel. Product appears
in `GET /listings`. Admin can edit per-SKU price and stock from the same edit dialog.
Manager sees those fields as read-only. TypeScript is happy.

---

## Phase 8 — Admin: Categories, Colors & Search Reindex

**Goal:** Admin panel gains category management (create/delete), color management
(update display name and hex), and a one-click search reindex trigger.

### 8.1 New API calls

**Categories (`entities/category/api/admin.ts`)**:
```ts
createCategory(name: string, parentId: number | null): Promise<CategoryDto>
  // POST /api/v1/admin/categories

deleteCategory(categoryId: number): Promise<void>
  // DELETE /api/v1/admin/categories/{categoryId}
  // 422 if category still has products — show error toast
```

**Colors (`entities/color/api.ts` — extend)**:
```ts
updateColor(colorId: number, displayName: string, hexCode: string): Promise<Color>
  // PATCH /api/v1/colors/{colorId}
```

**Search (`features/search/api/index.ts` — extend)**:
```ts
reindexSearch(): Promise<{ indexed: number; errorCount: number; message: string }>
  // POST /api/v1/search/reindex
```

### 8.2 New "Categories" Tab in Admin Panel

Add a fourth tab `categories` to the Tabs component in `manage/page.tsx`:

**`CategoryManagement`** component:
- Display the category tree (expandable nodes, uses `GET /api/v1/categories`)
- "New Category" form: name input + optional parent dropdown
  - calls `createCategory()`
  - invalidates category query on success
- Per-node "Delete" button (ADMIN only, hidden for MANAGER):
  - calls `deleteCategory(id)`
  - on `422`: toast "Category is still in use by products"
  - Confirm dialog before deletion

### 8.3 New "Colors" Tab in Admin Panel

Add a fifth tab `colors`:

**`ColorManagement`** component:
- Table of all colors: color swatch, key (read-only), display name (editable input),
  hex code (color picker + text input)
- "Save" button per row, enabled when display name or hex changed
  - calls `updateColor()`

### 8.4 New "Search" Card in Admin Panel (ADMIN only)

Add a small "Tools" card visible only to `user.role === "ADMIN"` — either as a sixth tab
or as a section below the tabs:

- Button: "Rebuild Search Index"
- On click: calls `reindexSearch()` with loading state
- On success: shows result inline: "Indexed 1,250 products (0 errors)"
- On error: toast with error message

### 8.5 Remove Stale ERP-era Strings in Admin UI

- Rename "ERP Synced Data" section header → "Catalog Data (read-only)"
- Remove/reword the "ERP-locked" tooltip text on Lock icons in the tier management table
- Replace "Managed by ERP" with "Managed by system" or remove the lock metaphors
  that no longer make sense now that ADMIN can edit price/stock directly

### 8.6 Definition of Done

Admin can create categories, delete categories, update color display names/hex codes.
ADMIN can trigger a search reindex and see the result. Manager sees categories and colors
tabs but cannot delete categories or trigger reindex. No ERP-era text remains in the UI.

---

## Cross-Cutting Concerns

### Error Handling (applies to all phases)

The backend returns a standard error envelope:
```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient stock for SKU RAD-0001-BLACK-M",
  "path": "/api/v1/cart/items"
}
```

Ensure `getErrorMessage(err)` in `shared/lib/` reads `err.response.data.message` for
these structured errors. Verify this works for the new endpoints (cart, checkout, payment).

### Authentication Guards (applies to all phases)

| Page/Feature | Required Role |
|---|---|
| Cart (drawer, add to cart button) | USER+ (any authenticated) |
| Checkout page | USER+ |
| Payment return page | public (no auth needed — provider callback) |
| Profile / orders | USER+ |
| Admin manage page | MANAGER+ |
| Create product | MANAGER+ |
| Edit SKU price/stock | ADMIN only |
| Delete category | ADMIN only |
| Color update | MANAGER+ |
| Search reindex | ADMIN only |

`ProtectedRoute` must accept `ADMIN` anywhere `MANAGER` is required
(ADMIN is a superset).

### FSD Dependency Direction

New slices must follow the downward import rule:
- `features/cart` may import from `entities/cart` and `shared`
- `features/checkout` may import from `features/cart`, `entities/cart`, `entities/loyalty`, `shared`
- `widgets/CartDrawer` may import from `features/cart`, `entities/cart`, `shared`
- No cross-feature imports (e.g., `features/cart` must not import from `features/checkout`)

---

## Quick Reference: New FSD Slices by Phase

| Phase | New Slice | Type |
|-------|-----------|------|
| 4 | `entities/cart` | Entity |
| 4 | `features/cart` | Feature |
| 4 | `widgets/CartDrawer` | Widget |
| 5 | `features/checkout` | Feature |
| 6 | `features/payment` | Feature |
| 7 | `features/product-creation` | Feature |
| 8 | `entities/color` | Entity |
| 8 | `entities/category/api/admin` | Entity extension |

## Quick Reference: New Pages by Phase

| Phase | Route | Description |
|-------|-------|-------------|
| 5 | `/checkout` | Cart → order conversion with loyalty points |
| 6 | `/payment/return` | Payment provider callback + status polling |

---

*Last updated: 2026-03-20*
