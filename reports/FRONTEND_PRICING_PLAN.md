# Frontend Pricing Implementation Plan

> **Branch:** `futureproving/removeErpAsSourceOfTruth`
> **Source:** `reports/FRONTEND_PRICING_IMPLEMENTATION.md` + backend state as of 2026-03-21
> **Constraint:** No backend code changes. Frontend only.

---

## Current State (baseline before implementation)

| File | Stale Fields In Use |
|------|-------------------|
| `entities/product/model/types.ts` | `Sku.price`, `ListingVariant.minPrice`, `ListingVariant.maxPrice`, `ListingVariant.tierDiscountedMinPrice` |
| `entities/product/ui/ProductCard.tsx` | `hasTierPrice`, `listing.tierDiscountedMinPrice`, `listing.minPrice`, `listing.maxPrice` — no sale badge |
| `entities/product/ui/ProductDetail.tsx` | `selectedSku?.price`, `listing.minPrice`, `listing.tierDiscountedMinPrice` — no per-SKU discount, no size dots |
| `shared/i18n/locales/*.json` | Missing keys: `loyaltyTierBadge`, `loyaltyTierDetail`, `partialDiscountHint` (all 3 locales) |
| `features/loyalty/ui/LoyaltyCard.tsx` | Null-tier message is generic "earnPoints" — no `spendToNextTier` amount shown |
| `frontend/CLAUDE.md` | Price Model section actively contradicts new API — says "Never render loyaltyPrice, saleTitle, saleColorHex" |

**What already works correctly (no changes needed):**
- `LoyaltyDashboard`, `TierProgress`, `TiersList` — all handle `tier: null` gracefully
- `features/loyalty/ui/LoyaltyCard.tsx` — null-tier fallback exists (just needs richer copy)
- All API layer endpoints — URLs are unchanged; only response shapes differ
- `HomeCollections`, `CollectionPage` — pass full `ListingVariant` object to `ProductCard` (verified)

---

## Phase 1 — TypeScript Types

**Status:** [ ] Pending

**Files:** `frontend/src/entities/product/model/types.ts`

**Changes:**

1. **`Sku` interface** — remove `price: number`, add full pricing block:
   - `originalPrice: number`
   - `discountPrice: number | null`
   - `discountPercentage: number | null`
   - `discountName: string | null`
   - `discountColorHex: string | null`
   - `loyaltyPrice: number | null`

2. **`ListingVariant` interface** — remove `minPrice`, `maxPrice`, `tierDiscountedMinPrice`, add:
   - `originalPrice: number`
   - `discountPrice: number | null`
   - `discountPercentage: number | null`
   - `discountName: string | null`
   - `discountColorHex: string | null`
   - `loyaltyPrice: number | null`
   - `loyaltyPercentage: number | null`
   - `isPartialDiscount: boolean`

3. **`ListingVariantDetail`** — confirm it still `extends ListingVariant` (inherits pricing automatically). No structural change; just verify no stale field overrides remain.

4. **Add `SiblingVariant` interface** (new):
   ```ts
   export interface SiblingVariant {
     slug: string;
     colorKey: string;
     colorHex: string | null;
     thumbnail: string | null;
   }
   ```
   Add `siblingVariants: SiblingVariant[]` to `ListingVariantDetail`.

**Why this is Phase 1:** All downstream compiler errors surface automatically after this. TypeScript strict mode turns every stale field reference into a compile error — acting as an automatic checklist for all remaining work.

---

## Phase 2 — Compiler Error Triage

**Status:** [ ] Pending

**Action:** Run `npm run build --prefix frontend` (or `tsc --noEmit` in the frontend directory) after Phase 1. The output will list every file that still references `minPrice`, `maxPrice`, `tierDiscountedMinPrice`, `Sku.price`, etc.

**Expected errors:**
- `ProductCard.tsx` — `listing.minPrice`, `listing.maxPrice`, `listing.tierDiscountedMinPrice`, `hasTierPrice`
- `ProductDetail.tsx` — `selectedSku?.price`, `listing.minPrice`, `listing.tierDiscountedMinPrice`
- Potentially any other components that destructure `ListingVariant` fields

**Deliverable:** A confirmed list of every file that needs a change. No code changes in this phase — triage only.

---

## Phase 3 — ProductCard Rewrite

**Status:** [ ] Pending

**File:** `frontend/src/entities/product/ui/ProductCard.tsx`

**Changes:**

1. **Remove** `hasTierPrice` derived boolean. Replace with:
   ```ts
   const hasDiscount    = listing.discountPrice != null;
   const hasLoyalty     = listing.loyaltyPrice != null;
   const hasCheaperPrice = hasDiscount || hasLoyalty;
   ```

2. **Badge row** — add a sale/discount badge alongside the existing `topSelling` badge:
   - Background: `listing.discountColorHex ?? '#ef4444'`
   - Label: `{listing.discountName} · -{listing.discountPercentage}%`
   - Sub-label when `listing.isPartialDiscount`: `"· select sizes"`
   - Render only when `hasDiscount`

3. **Tier image overlay badge** — replace `hasTierPrice && "Crown Your Price"` with `hasLoyalty && Crown + tc("yourPrice")`.

4. **Price section — full rewrite** implementing the 4-case table:

   | User type | Hero price | Strikethrough | Sale badge | Loyalty badge |
   |-----------|-----------|---------------|------------|---------------|
   | Guest / no tier, no sale | `originalPrice` (violet) | — | — | — |
   | Guest / no tier, sale active | `discountPrice` (red) | `originalPrice` | `-X%` colored | — |
   | Auth + tier, no sale | `loyaltyPrice` (amber + Crown) | `originalPrice` | — | tier `%` badge |
   | Auth + tier, sale active | `loyaltyPrice` (amber + Crown) | `originalPrice` | sale badge | tier `%` badge |

   - Loyalty tier badge line (below strikethrough): uses `tc("loyaltyTierBadge", { pct: listing.loyaltyPercentage })` — renders only when `hasLoyalty && listing.loyaltyPercentage != null`.

---

## Phase 4 — ProductDetail Rewrite

**Status:** [ ] Pending

**File:** `frontend/src/entities/product/ui/ProductDetail.tsx`

**Changes:**

1. **Price computation block** — replace `skuPrice`, `displayPrice`, `hasTierPrice` with per-SKU fallback pattern:
   ```ts
   const activeOriginal    = selectedSku?.originalPrice    ?? listing.originalPrice;
   const activeDiscount    = selectedSku?.discountPrice    ?? listing.discountPrice;
   const activeDiscountPct = selectedSku?.discountPercentage ?? listing.discountPercentage;
   const activeDiscountName = selectedSku?.discountName   ?? listing.discountName;
   const activeDiscountHex  = selectedSku?.discountColorHex ?? listing.discountColorHex;
   const activeLoyalty     = selectedSku?.loyaltyPrice    ?? listing.loyaltyPrice;

   const hasDiscount     = activeDiscount != null;
   const hasLoyalty      = activeLoyalty != null;
   const hasCheaperPrice = hasDiscount || hasLoyalty;
   ```

2. **Price block** — full rewrite of the `hasTierPrice ? ... : ...` JSX block. Same 4-case logic as ProductCard but larger typography (`text-2xl sm:text-[2rem]`). Additions specific to the detail page:
   - Sale campaign badge rendered inline with the hero price (name + `%`)
   - `listing.isPartialDiscount && !selectedSku` → show `t("partialDiscountHint")` ("Sale applies to select sizes only")

3. **Size selector** — add per-SKU discount signal: a small colored dot (`w-2 h-2 rounded-full`) with `skuItem.discountColorHex` positioned absolute top-right of each button when `skuItem.discountPrice != null`.

**Key behavior:** When a SKU is selected, pricing swaps to that SKU's exact data — discount badge may disappear for non-discounted sizes, or a different campaign badge may appear for mixed-discount variants.

---

## Phase 5 — i18n Keys

**Status:** [ ] Pending

**Files:** `frontend/src/shared/i18n/locales/en.json`, `ru.json`, `tj.json`

**Keys to add:**

| Namespace | Key | EN | RU | TJ |
|-----------|-----|----|-----|-----|
| `common` | `loyaltyTierBadge` | `"{pct}% tier discount"` | `"Скидка уровня {pct}%"` | `"Тахфифи сатҳ {pct}%"` |
| `common` | `loyaltyTierDetail` | `"Your loyalty tier · {pct}% off applied"` | `"Ваш уровень лояльности · скидка {pct}%"` | `"Сатҳи вафодории шумо · {pct}% тахфиф"` |
| `productDetail` | `partialDiscountHint` | `"Sale price applies to select sizes only"` | `"Цена со скидкой действует только на отдельные размеры"` | `"Нархи тахфифӣ танҳо барои андозаҳои муайян татбиқ мешавад"` |

**Note:** `common.yourPrice` already exists in all 3 locales — do not add again.

---

## Phase 6 — Loyalty Dashboard Null-Tier Incentive

**Status:** [ ] Pending

**File:** `frontend/src/features/loyalty/ui/LoyaltyCard.tsx`

**Current state:** When `tier === null`, shows a generic `t("earnPoints")` string with default purple gradient.

**Change:** Enhance the null-tier display to show the `spendToNextTier` amount when available:
- Keep the existing fallback gradient (no design change needed at the card level)
- Replace the generic `earnPoints` paragraph with a richer incentive block:
  - Title: "Start earning loyalty rewards"
  - Sub-text: "Spend {formatPrice(loyalty.spendToNextTier)} more this month to unlock tier discounts" — rendered only when `loyalty.spendToNextTier != null`
  - Fallback (no spend data): keep current `t("earnPoints")` message

**No changes needed to:** `LoyaltyDashboard`, `TierProgress`, `TiersList` — all handle null tier correctly already.

---

## Phase 7 — frontend/CLAUDE.md Update

**Status:** [ ] Pending

**File:** `frontend/CLAUDE.md`

**Change:** Replace the `### Price Model` section under `## Backend API Contract` entirely. The current text:
- Still documents `minPrice`, `maxPrice`, `tierDiscountedMinPrice`
- Actively instructs "Never render `discountedPrice`, `loyaltyPrice`, `saleTitle`, or `saleColorHex`" — now **the opposite of correct**

Replace with the new Price Model table (from `FRONTEND_PRICING_IMPLEMENTATION.md` Part 6), covering:
- Variant-level fields table (8 fields)
- SKU-level fields (same set minus `isPartialDiscount`)
- Display logic 4-case table
- "Formula for reference — computed on backend, do not recompute on frontend" note

---

## Phase Completion Log

| Phase | Status | Summary |
|-------|--------|---------|
| Phase 1 — TypeScript Types | [x] | Removed `Sku.price`, `ListingVariant.minPrice/maxPrice/tierDiscountedMinPrice`. Added full 6-field pricing block to `Sku`, 8-field pricing block to `ListingVariant`, new `SiblingVariant` interface, and `siblingVariants` field on `ListingVariantDetail`. |
| Phase 2 — Compiler Error Triage | [x] | 23 errors found across 3 files. 3 orphan errors in `(admin)/manage/page.tsx` fixed immediately (`minPrice → originalPrice`, `sku.price → sku.originalPrice`). 20 errors remain in ProductCard (10) and ProductDetail (10) — resolved by Phases 3 & 4. |
| Phase 3 — ProductCard Rewrite | [x] | Replaced `hasTierPrice` with `hasDiscount`/`hasLoyalty`/`hasCheaperPrice`. Added sale badge (name + % + "select sizes" hint) stacked below topSelling in top-left. Image overlay: `hasTierPrice → hasLoyalty`. Price section: full 4-case rewrite — loyalty amber hero, discount red hero, original violet; strikethrough + colored `-X%` badge when cheaper price exists; loyalty tier `%` label. 0 compiler errors in ProductCard. |
| Phase 4 — ProductDetail Rewrite | [x] | Replaced `skuPrice`/`displayPrice`/`hasTierPrice` with 5 `active*` per-SKU fallback variables + `hasDiscount`/`hasLoyalty`/`hasCheaperPrice`. Image overlay: `hasTierPrice → hasLoyalty`. Price block: full 4-case rewrite — loyalty amber hero (with inline sale badge when both apply), discount red hero, original violet; strikethrough + `-X%` badge (only when `hasDiscount && !hasLoyalty`); loyalty tier detail label; partial discount hint. Size selector: colored dot on each SKU with an active discount. 0 compiler errors across entire project. |
| Phase 5 — i18n Keys | [ ] | |
| Phase 6 — Loyalty Dashboard Null-Tier Incentive | [ ] | |
| Phase 7 — frontend/CLAUDE.md Update | [ ] | |
