# Frontend Pricing Implementation Report

> **Scope:** Changes required in the frontend to reflect the backend pricing redesign
> implemented from CHANGES_DISCUSSION.md sections 1, 2, and 3.
>
> **Do not touch any backend code.** This report is purely frontend work.
> **Do not change existing code while reading this report.** Implement each section in order.

---

## What Changed on the Backend

Three backend features are now live:

1. **Section 1 — Rich pricing fields on every listing response**
2. **Section 2 — Loyalty tier is no longer assigned at registration** (users start with `tier: null`)
3. **Section 3 — Discount redesign** (per-SKU, type hierarchy, best-of loyalty vs sale)

The frontend currently uses stale field names and stale pricing logic. Everything in this
report must be updated before the pricing UI works correctly.

---

## Part 1 — TypeScript Type Updates

**File:** `frontend/src/entities/product/model/types.ts`

### 1a. `Sku` interface

Remove `price`. Add the full pricing block. All discount/loyalty fields are nullable —
null means "not applicable for this SKU / this user".

```ts
export interface Sku {
  skuId: number;
  skuCode: string;
  sizeLabel: string;
  stockQuantity: number;
  // Pricing — mirrors backend SkuDto exactly
  originalPrice: number;
  discountPrice: number | null;       // null = no active discount on this SKU
  discountPercentage: number | null;  // whole number, e.g. 20 means 20%
  discountName: string | null;
  discountColorHex: string | null;
  loyaltyPrice: number | null;        // null for guests / users without a tier
}
```

### 1b. `ListingVariant` interface

Remove `minPrice`, `maxPrice`, `tierDiscountedMinPrice`. Add the new fields.

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
  skus: Sku[];
  // Pricing
  originalPrice: number;              // always present; pre-discount price of cheapest SKU
  discountPrice: number | null;       // null = no active sale on this variant
  discountPercentage: number | null;  // whole number, null if no sale
  discountName: string | null;        // e.g. "Winter Collection", null if no sale
  discountColorHex: string | null;    // e.g. "#1e40af", null if no sale
  loyaltyPrice: number | null;        // null for guests / no-tier users
  loyaltyPercentage: number | null;   // user's own tier %, null for guests / no-tier
  isPartialDiscount: boolean;         // true = only some sizes are on sale
}
```

### 1c. `ListingVariantDetail` interface

`ListingVariantDetail extends ListingVariant` already, so it inherits all the new
pricing fields automatically. No structural change needed — just verify the `extends`
is still in place and remove any old price field overrides if they existed.

The detail response also includes `siblingVariants` from the backend. Add it if missing:

```ts
export interface SiblingVariant {
  slug: string;
  colorKey: string;
  colorHex: string | null;
  thumbnail: string | null;
}

export interface ListingVariantDetail extends ListingVariant {
  webDescription: string | null;
  attributes: Attribute[];
  siblingVariants: SiblingVariant[];
}
```

### 1d. `LoyaltyProfile` — tier can now be null from day one

The backend no longer assigns a tier at registration. `tier: null` means the user
has not yet earned a tier. The type already allows `tier: LoyaltyTier | null` — confirm
this is handled everywhere the profile is rendered (profile page, loyalty dashboard).

---

## Part 2 — ProductCard Rewrite

**File:** `frontend/src/entities/product/ui/ProductCard.tsx`

### What exists now (stale)

- `hasTierPrice = listing.tierDiscountedMinPrice != null` → **delete this**
- Price section uses `listing.minPrice`, `listing.maxPrice`, `listing.tierDiscountedMinPrice` → **all deleted**
- No sale discount badge rendered anywhere

### What to implement

#### Derived booleans (replace old ones)

```ts
const hasDiscount = listing.discountPrice != null;
const hasLoyalty  = listing.loyaltyPrice != null;
const hasCheaperPrice = hasDiscount || hasLoyalty;
```

#### Top badge row — add a sale badge

Currently the badge row has: `topSelling` badge on the left, color dot on the right.

Add a **sale/discount badge** on the left, below or alongside `topSelling`.
Use `discountColorHex` as the background, `discountName` as the label,
and `discountPercentage` as the suffix. If `isPartialDiscount` is true,
add a small "on select sizes" sub-label.

```tsx
{hasDiscount && (
  <Badge
    style={{ backgroundColor: listing.discountColorHex ?? '#ef4444' }}
    className="text-white text-[9px] sm:text-xs shrink-0"
  >
    {listing.discountName} · -{listing.discountPercentage}%
    {listing.isPartialDiscount && (
      <span className="font-normal opacity-80"> · select sizes</span>
    )}
  </Badge>
)}
```

Place this in the top-left corner area alongside the `topSelling` badge
(e.g. stack vertically or show whichever is relevant).

#### Tier price badge on image

Replace:
```tsx
{hasTierPrice && (
  <span>Crown Your Price</span>
)}
```
With:
```tsx
{hasLoyalty && (
  <span className="... bg-amber-500 ...">
    <Crown /> {tc("yourPrice")}
  </span>
)}
```

#### Price section — full rewrite

Replace the entire `hasTierPrice ? (...) : (...)` block with:

```tsx
<div className="mt-auto pt-1 sm:pt-1.5 flex flex-col gap-0.5">

  {/* Hero price row */}
  <div className="flex items-center justify-between gap-1">
    <div className="flex items-baseline gap-1 sm:gap-1.5 min-w-0">

      {hasLoyalty ? (
        /* Auth + tier: loyalty price is hero */
        <>
          <Crown className="h-3 w-3 sm:h-4 sm:w-4 text-amber-500 shrink-0" />
          <span className="text-sm sm:text-lg font-bold text-amber-600 truncate">
            {formatPrice(listing.loyaltyPrice!)}
          </span>
          <span className="hidden sm:inline text-xs font-medium text-amber-600/70">
            {tc("yourPrice")}
          </span>
        </>
      ) : hasDiscount ? (
        /* Sale price is hero */
        <span className="text-base sm:text-lg font-bold text-red-600">
          {formatPrice(listing.discountPrice!)}
        </span>
      ) : (
        /* Full price */
        <span className="text-base sm:text-lg font-bold text-violet-600">
          {formatPrice(listing.originalPrice)}
        </span>
      )}

    </div>

    {/* Low stock label */}
    {isLowStock && (
      <span className="text-[10px] sm:text-xs font-medium text-orange-600 whitespace-nowrap shrink-0">
        {tc("lowStock", { count: stock })}
      </span>
    )}
  </div>

  {/* Strikethrough row — only when a cheaper price exists */}
  {hasCheaperPrice && (
    <div className="flex items-baseline gap-1">
      <span className="text-[10px] sm:text-sm text-muted-foreground line-through">
        {formatPrice(listing.originalPrice)}
      </span>
      {/* Show discount % badge if sale applies */}
      {hasDiscount && (
        <span
          className="text-[9px] sm:text-xs font-semibold px-1 rounded text-white"
          style={{ backgroundColor: listing.discountColorHex ?? '#ef4444' }}
        >
          -{listing.discountPercentage}%
        </span>
      )}
    </div>
  )}

  {/* Loyalty tier badge — shows tier name + its own % */}
  {hasLoyalty && listing.loyaltyPercentage != null && (
    <span className="text-[9px] sm:text-xs text-amber-600 font-medium">
      {tc("loyaltyTierBadge", { pct: listing.loyaltyPercentage })}
      {/* e.g. "Gold · 5% off" */}
    </span>
  )}

</div>
```

**Logic summary for the card price section:**

| User type | Hero price | Strikethrough | Sale badge | Loyalty badge |
|-----------|-----------|---------------|------------|---------------|
| Guest / no tier, no sale | `originalPrice` | — | — | — |
| Guest / no tier, sale active | `discountPrice` (red) | `originalPrice` | `-X%` | — |
| Auth + tier, no sale | `loyaltyPrice` (amber) | `originalPrice` | — | "Gold · 5% off" |
| Auth + tier, sale active | `loyaltyPrice` (amber) | `originalPrice` | sale badge | "Gold · 5% off" |

---

## Part 3 — ProductDetail Rewrite

**File:** `frontend/src/entities/product/ui/ProductDetail.tsx`

### Price computation — replace entirely

Delete:
```ts
const skuPrice = selectedSku?.price ?? null;
const displayPrice = skuPrice ?? listing.minPrice;
const hasTierPrice = listing.tierDiscountedMinPrice != null;
```

Replace with:
```ts
// When a SKU is selected, use its per-SKU pricing. Otherwise use variant-level.
const activeOriginal    = selectedSku?.originalPrice    ?? listing.originalPrice;
const activeDiscount    = selectedSku?.discountPrice    ?? listing.discountPrice;
const activeDiscountPct = selectedSku?.discountPercentage ?? listing.discountPercentage;
const activeDiscountName = selectedSku?.discountName    ?? listing.discountName;
const activeDiscountHex  = selectedSku?.discountColorHex ?? listing.discountColorHex;
const activeLoyalty     = selectedSku?.loyaltyPrice     ?? listing.loyaltyPrice;

const hasDiscount  = activeDiscount != null;
const hasLoyalty   = activeLoyalty != null;
const hasCheaperPrice = hasDiscount || hasLoyalty;
```

When no size is selected: uses variant-level pricing (the cheapest-when-discounted SKU's data).
When a size is selected: uses that SKU's exact pricing — discount may appear or disappear
depending on whether that size is part of an active campaign.

### Price block — full rewrite

Replace the entire `hasTierPrice ? (...) : (...)` price block:

```tsx
<div className="bg-muted/40 rounded-xl px-3 py-3 sm:p-4 space-y-2">

  {/* Hero price */}
  <div className="flex items-center gap-2 flex-wrap">
    {hasLoyalty ? (
      <>
        <Crown className="h-4 w-4 sm:h-5 sm:w-5 text-amber-500 shrink-0" />
        <span className="text-2xl sm:text-[2rem] font-bold text-amber-600">
          {formatPrice(activeLoyalty!)}
        </span>
        <span className="text-xs sm:text-sm font-medium text-amber-600/70">
          {tc("yourPrice")}
        </span>
      </>
    ) : hasDiscount ? (
      <>
        <span className="text-2xl sm:text-[2rem] font-bold text-red-600">
          {formatPrice(activeDiscount!)}
        </span>
        {/* Sale campaign badge */}
        <span
          className="text-xs font-semibold px-2 py-0.5 rounded-full text-white"
          style={{ backgroundColor: activeDiscountHex ?? '#ef4444' }}
        >
          {activeDiscountName} · -{activeDiscountPct}%
        </span>
      </>
    ) : (
      <span className="text-2xl sm:text-[2rem] font-bold text-violet-600">
        {formatPrice(activeOriginal)}
      </span>
    )}
  </div>

  {/* Strikethrough row */}
  {hasCheaperPrice && (
    <div className="flex items-baseline gap-2">
      <span className="text-sm text-muted-foreground line-through">
        {formatPrice(activeOriginal)}
      </span>
      {hasDiscount && !hasLoyalty && (
        <span
          className="text-xs font-semibold px-1.5 py-0.5 rounded text-white"
          style={{ backgroundColor: activeDiscountHex ?? '#ef4444' }}
        >
          -{activeDiscountPct}%
        </span>
      )}
    </div>
  )}

  {/* Loyalty tier badge */}
  {hasLoyalty && listing.loyaltyPercentage != null && (
    <p className="text-xs text-amber-600 font-medium">
      {/* e.g. "Your Gold tier · 5% off applied" */}
      {tc("loyaltyTierDetail", { pct: listing.loyaltyPercentage })}
    </p>
  )}

  {/* Partial discount hint — shown at variant level when no SKU selected */}
  {listing.isPartialDiscount && !selectedSku && (
    <p className="text-xs text-muted-foreground">
      {t("partialDiscountHint")} {/* "Sale applies to select sizes only" */}
    </p>
  )}

</div>
```

### Size selector — show per-SKU discount signal

The size buttons currently only show the label and a strikethrough if out of stock.
Update each button to indicate whether that size has a discount:

```tsx
{listing.skus.map((skuItem) => {
  const isOutOfStock   = skuItem.stockQuantity === 0;
  const isSelected     = selectedSku?.skuId === skuItem.skuId;
  const skuHasDiscount = skuItem.discountPrice != null;

  return (
    <button key={skuItem.skuId} ... className={`... ${isSelected ? '...' : '...'}`}>
      {skuItem.sizeLabel}
      {/* Small colored dot to signal this size is on sale */}
      {skuHasDiscount && (
        <span
          className="absolute -top-1 -right-1 w-2 h-2 rounded-full border border-background"
          style={{ backgroundColor: skuItem.discountColorHex ?? '#ef4444' }}
        />
      )}
      {isOutOfStock && ( /* existing strikethrough */ )}
    </button>
  );
})}
```

This gives the user a subtle visual cue that certain sizes are on sale before they click.

---

## Part 4 — i18n Keys to Add

**Files:** `frontend/src/shared/i18n/` (whichever locale files exist, e.g. `en.json`, `ru.json`, `tj.json`)

Add the following keys to the `common` and `productDetail` namespaces:

```json
// common namespace
"yourPrice": "Your Price",
"loyaltyTierBadge": "{pct}% tier discount",
"loyaltyTierDetail": "Your loyalty tier · {pct}% off applied"

// productDetail namespace
"partialDiscountHint": "Sale price applies to select sizes only"
```

Adjust wording for Russian and Tajik locales accordingly.

---

## Part 5 — Loyalty Dashboard / Profile Page

**File:** `frontend/src/widgets/loyalty-dashboard/LoyaltyDashboard.tsx`
**File:** `frontend/src/features/loyalty/ui/LoyaltyCard.tsx`
**File:** `frontend/src/features/loyalty/ui/TierProgress.tsx`

### What changed (backend Section 2)

Users now register with **no tier** (`tier: null`). The loyalty profile still exists
from day one but `tier` is null until the user earns/is assigned one.

### Required UI changes

**When `loyalty.tier === null`:**

Do NOT show a tier badge or tier name. Show instead an incentive message:

```tsx
{loyalty.tier === null ? (
  <div className="rounded-lg border border-dashed border-amber-300 bg-amber-50 p-4 text-sm text-amber-800">
    <p className="font-semibold">Start earning loyalty rewards</p>
    {loyalty.spendToNextTier != null && (
      <p className="mt-1 text-amber-700">
        Spend {formatPrice(loyalty.spendToNextTier)} more this month to
        enter the loyalty program and unlock tier discounts.
      </p>
    )}
  </div>
) : (
  /* Existing tier display — name, color, discountPercentage badge */
  <TierBadge tier={loyalty.tier} />
)}
```

**`TierProgress` component:**

Currently may assume `tier` is always present. Guard all `loyalty.tier.*` accesses.
When tier is null, show a progress bar toward the first tier (using `spendToNextTier`
and `currentMonthSpending`).

**`LoyaltyCard` component:**

The "Gold tier member" or similar title line must not render when `tier === null`.
Replace with "No tier yet" or the incentive message.

---

## Part 6 — frontend/CLAUDE.md — Price Model Section Must Be Updated

**File:** `frontend/CLAUDE.md`

The `### Price Model` section under `## Backend API Contract` is now **entirely stale**.
It still documents `minPrice`, `maxPrice`, `tierDiscountedMinPrice` and says
"Never render `discountedPrice`, `loyaltyPrice`, `saleTitle`, or `saleColorHex`"
— which is the opposite of what should now be rendered.

Replace the Price Model section with:

```md
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
```

---

## Part 7 — Query Key and API Layer

**File:** `frontend/src/entities/product/api/index.ts`

No API URL changes are needed — the endpoints are identical:
- `GET /api/v1/listings` — grid
- `GET /api/v1/listings/{slug}` — detail
- `GET /api/v1/listings/search?q=...` — search

The response shapes change (new fields). Because TypeScript is strict, fixing the
types in Part 1 will surface every place old field names are referenced via type errors.
Use those compiler errors as a checklist.

**Recommendation:** After updating types, run `npm run build` or `tsc --noEmit` and
fix every type error before touching component logic.

---

## Part 8 — HomeCollections and CollectionPage

**Files:**
- `frontend/src/widgets/HomeCollections/ui/HomeCollections.tsx`
- `frontend/src/app/(storefront)/collections/[key]/page.tsx`

Both render `ListingVariant[]` via `ProductCard`. Since `ProductCard` is being updated
in Part 2, no structural changes are needed in these files. However, verify they are
passing the full `ListingVariant` object (not a subset) to `ProductCard` — so the new
fields flow through automatically.

---

## Summary Checklist

| # | File | What to do |
|---|------|------------|
| 1 | `entities/product/model/types.ts` | Update `Sku`, `ListingVariant`, add `SiblingVariant` |
| 2 | `entities/product/ui/ProductCard.tsx` | Rewrite price section, add sale badge, update tier logic |
| 3 | `entities/product/ui/ProductDetail.tsx` | Rewrite price block, add per-SKU swap, add size discount dots |
| 4 | `shared/i18n/*.json` | Add `loyaltyTierBadge`, `loyaltyTierDetail`, `partialDiscountHint` keys |
| 5 | `widgets/loyalty-dashboard/LoyaltyDashboard.tsx` | Handle `tier === null` state |
| 6 | `features/loyalty/ui/LoyaltyCard.tsx` | Guard all `tier.*` accesses, null-tier incentive message |
| 7 | `features/loyalty/ui/TierProgress.tsx` | Handle no-tier progress toward first tier |
| 8 | `frontend/CLAUDE.md` | Replace stale Price Model section |

**Start with #1 (types) and run the TypeScript compiler.** The type errors will guide
you to every component and hook that references stale field names.
