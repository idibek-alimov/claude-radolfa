# Pricing API Guide for Frontend

> Last updated: 2026-03-15
> Audience: Frontend developers integrating the dual-discount pricing system

---

## Table of Contents

1. [Quick Summary](#1-quick-summary)
2. [The Three Price Fields](#2-the-three-price-fields)
3. [Display Logic Decision Tree](#3-display-logic-decision-tree)
4. [API Endpoints That Return Pricing](#4-api-endpoints-that-return-pricing)
5. [DTO Schemas](#5-dto-schemas)
6. [Pricing Calculation Formula](#6-pricing-calculation-formula)
7. [Concrete Examples by Scenario](#7-concrete-examples-by-scenario)
8. [Seed Data Test Matrix](#8-seed-data-test-matrix)
9. [Authentication & How Loyalty Price Appears](#9-authentication--how-loyalty-price-appears)
10. [Loyalty Tiers Reference](#10-loyalty-tiers-reference)
11. [Frontend Display Recommendations](#11-frontend-display-recommendations)
12. [Migration Notes (Breaking Changes from Old API)](#12-migration-notes-breaking-changes-from-old-api)

---

## 1. Quick Summary

Every product now has **up to three price levels** and **two discount percentages**:

| Field | Source | Nullable? | Meaning |
|-------|--------|-----------|---------|
| `originalPrice` | ERPNext standard rate | No | The full retail / list price |
| `discountedPrice` | ERPNext product discount | **Yes** | Price after ERP promotion (e.g. seasonal sale). `null` = no active discount |
| `loyaltyPrice` | Computed at runtime | **Yes** | Price after applying the logged-in user's loyalty tier discount on top of `discountedPrice` (or `originalPrice` if no ERP discount). `null` = user not logged in, has no tier, or tier discount is 0% |
| `discountPercentage` | ERPNext (synced) | **Yes** | The ERP product discount % (e.g. `15.00`). `null` = no active discount. Use for badges like "-15%" |
| `loyaltyDiscountPercentage` | Computed at runtime | **Yes** | The user's loyalty tier discount % (e.g. `5.00`). `null` = not logged in or no tier. Use for badges like "Your GOLD tier: extra 5% off" |

**The stacking formula:**

```
loyaltyPrice = (discountedPrice ?? originalPrice) × (1 − loyaltyDiscountPercentage%)
```

Discounts **multiply** (stack), they don't add. A 15% ERP discount + 20% loyalty discount ≠ 35% off. It's:
`originalPrice × 0.85 × 0.80 = 32% total off`.

---

## 2. The Three Price Fields

### `originalPrice` — Always present
- The base retail price from ERPNext (`standard_rate`).
- Never null for products with SKUs.
- This is the "was" price when there's a discount, or the only price when there's none.

### `discountedPrice` — Null when no ERP discount is active
- Set when ERPNext has a product-level promotion running.
- **Expiry-aware**: if the discount's `discounted_ends_at` timestamp has passed, the backend returns `null` (as if no discount exists). The frontend never needs to check expiry.
- When present, it is always **less than** `originalPrice`.

### `loyaltyPrice` — Null for anonymous users and users without a tier
- Computed server-side by `TierPricingEnricher` based on the authenticated user's loyalty tier.
- The base for calculation is `discountedPrice` if it exists, otherwise `originalPrice`.
- When present, it is always **less than or equal to** whichever price it was derived from.

### Nullability Matrix

| User State | `originalPrice` | `discountedPrice` | `loyaltyPrice` | `discountPercentage` | `loyaltyDiscountPercentage` |
|---|---|---|---|---|---|
| Anonymous, no ERP discount | ✅ | `null` | `null` | `null` | `null` |
| Anonymous, ERP discount active | ✅ | ✅ | `null` | ✅ (e.g. `15.00`) | `null` |
| Logged in (no tier), no ERP discount | ✅ | `null` | `null` | `null` | `null` |
| Logged in (no tier), ERP discount active | ✅ | ✅ | `null` | ✅ | `null` |
| Logged in (has tier), no ERP discount | ✅ | `null` | ✅ | `null` | ✅ (e.g. `5.00`) |
| Logged in (has tier), ERP discount active | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 3. Display Logic Decision Tree

Use this to decide what to render on a product card or detail page:

```
if (loyaltyPrice != null) {
    // User is logged in with an active loyalty tier
    show loyaltyPrice as the MAIN price

    if (discountedPrice != null) {
        // Three prices: original → discounted → loyalty
        show originalPrice with strikethrough
        show discountedPrice with strikethrough (smaller)
        show loyaltyPrice as current price
        show badge: `-${discountPercentage}%` (e.g. "-15%")
        show badge: `Your tier: extra ${loyaltyDiscountPercentage}% off`
    } else {
        // Two prices: original → loyalty
        show originalPrice with strikethrough
        show loyaltyPrice as current price
        show badge: `${loyaltyDiscountPercentage}% Loyalty Discount`
    }

} else if (discountedPrice != null) {
    // ERP discount only (anonymous or user without tier)
    show originalPrice with strikethrough
    show discountedPrice as current price
    show badge: `-${discountPercentage}%` (e.g. "-15%")

} else {
    // Full price — no discounts
    show originalPrice as current price
    no badge
}
```

---

## 4. API Endpoints That Return Pricing

### Product Grid (cards with aggregated pricing)

| Endpoint | Method | Params | Returns |
|----------|--------|--------|---------|
| `/api/v1/listings` | GET | `page` (default=1), `limit` (default=12) | `PageResult<ListingVariantDto>` |
| `/api/v1/listings/search` | GET | `q` (required), `page`, `limit` | `PageResult<ListingVariantDto>` |
| `/api/v1/categories/{slug}/products` | GET | `page`, `limit` | `PageResult<ListingVariantDto>` |

### Product Detail (full info with SKUs)

| Endpoint | Method | Params | Returns |
|----------|--------|--------|---------|
| `/api/v1/listings/{slug}` | GET | — | `ListingVariantDetailDto` |

### Homepage Collections

| Endpoint | Method | Params | Returns |
|----------|--------|--------|---------|
| `/api/v1/home/collections` | GET | — | `List<HomeSectionDto>` |
| `/api/v1/home/collections/{key}` | GET | `page`, `limit` | `CollectionPageDto` |

Collection keys: `featured`, `new_arrivals`, `on_sale`

### Loyalty Info

| Endpoint | Method | Params | Returns |
|----------|--------|--------|---------|
| `/api/v1/loyalty-tiers` | GET | — | `List<LoyaltyTierDto>` |

All GET endpoints above are **public** — no authentication required. But `loyaltyPrice` will only be populated when the request includes a valid JWT.

---

## 5. DTO Schemas

### ListingVariantDto (product card)

```typescript
interface ListingVariantDto {
  id: number;
  slug: string;
  name: string;
  category: string | null;
  colorKey: string;
  colorHexCode: string | null;
  webDescription: string | null;
  images: string[];
  originalPrice: number;              // Always present
  discountedPrice: number | null;     // null = no active ERP discount
  loyaltyPrice: number | null;        // null = not logged in or no tier
  discountPercentage: number | null;  // ERP discount % (e.g. 15.00). null = no active discount
  loyaltyDiscountPercentage: number | null; // User's tier discount % (e.g. 5.00). null = no tier
  totalStock: number;
  topSelling: boolean;
  featured: boolean;
}
```

### ListingVariantDetailDto (product detail page)

```typescript
interface ListingVariantDetailDto {
  id: number;
  slug: string;
  name: string;
  category: string | null;
  colorKey: string;
  colorHexCode: string | null;
  webDescription: string | null;
  images: string[];
  originalPrice: number;              // MIN across all SKUs
  discountedPrice: number | null;     // MIN across active-discount SKUs
  loyaltyPrice: number | null;
  discountPercentage: number | null;  // ERP discount % (MIN across active SKUs)
  loyaltyDiscountPercentage: number | null; // User's tier discount %
  totalStock: number;
  topSelling: boolean;
  featured: boolean;
  skus: SkuDto[];
  siblingVariants: SiblingVariant[];
}

interface SiblingVariant {
  slug: string;
  colorKey: string;
  colorHexCode: string | null;
  thumbnail: string | null;
}
```

### SkuDto (individual size/price unit)

```typescript
interface SkuDto {
  id: number;
  erpItemCode: string;
  sizeLabel: string;
  stockQuantity: number;
  originalPrice: number;              // This SKU's list price
  discountedPrice: number | null;     // null if discount expired or doesn't exist
  loyaltyPrice: number | null;        // null if not logged in or no tier
  discountPercentage: number | null;  // ERP discount % for this SKU (e.g. 15.00)
  loyaltyDiscountPercentage: number | null; // User's tier discount %
  onSale: boolean;                    // true only if discountedPrice is active
  discountedEndsAt: string | null;    // ISO-8601 instant (e.g. "2026-03-29T00:00:00Z")
}
```

### HomeSectionDto

```typescript
interface HomeSectionDto {
  key: string;                        // "featured" | "new_arrivals" | "on_sale"
  title: string;                      // Display name
  items: ListingVariantDto[];
}
```

### CollectionPageDto

```typescript
interface CollectionPageDto {
  key: string;
  title: string;
  page: PageResult<ListingVariantDto>;
}
```

### PageResult\<T\>

```typescript
interface PageResult<T> {
  items: T[];
  totalElements: number;
  page: number;                       // 1-based
  hasMore: boolean;
}
```

### LoyaltyTierDto

```typescript
interface LoyaltyTierDto {
  id: number;
  name: string;                       // "GOLD" | "PLATINUM" | "TITANIUM"
  discountPercentage: number;         // 5.00, 15.00, 20.00
  cashbackPercentage: number;
  minSpendRequirement: number;
  displayOrder: number;               // 1 = highest tier
  color: string;                      // Hex code: "#C8962D"
}
```

---

## 6. Pricing Calculation Formula

The backend handles all calculations. This section is for **understanding/verification only** — the frontend should never compute prices.

```
Step 1 — ERP discount (applied at data layer):
  discountedPrice = originalPrice × (1 − erpDiscountPercent / 100)
  Example: 500 × (1 − 15/100) = 500 × 0.85 = 425.00

Step 2 — Loyalty discount (applied by TierPricingEnricher):
  base = discountedPrice ?? originalPrice
  loyaltyPrice = base × (1 − loyaltyDiscountPercent / 100)
  Example: 425 × (1 − 20/100) = 425 × 0.80 = 340.00

Total effective discount:
  1 − (loyaltyPrice / originalPrice) = 1 − (340 / 500) = 32%
  (NOT 15% + 20% = 35%)
```

---

## 7. Concrete Examples by Scenario

### Scenario A: No discounts, anonymous user
**Product: Essential Cotton T-Shirt ($25)**

```json
{
  "originalPrice": 25.00,
  "discountedPrice": null,
  "loyaltyPrice": null,
  "discountPercentage": null,
  "loyaltyDiscountPercentage": null
}
```
Display: **$25.00**

---

### Scenario B: No ERP discount, GOLD user (5% loyalty)
**Product: Essential Cotton T-Shirt ($25)**

```json
{
  "originalPrice": 25.00,
  "discountedPrice": null,
  "loyaltyPrice": 23.75,
  "discountPercentage": null,
  "loyaltyDiscountPercentage": 5.00
}
```
Display: ~~$25.00~~ → **$23.75** | Badge: "5% Loyalty Discount"

---

### Scenario C: 15% ERP discount, anonymous user
**Product: Tailored Business Suit ($250)**

```json
{
  "originalPrice": 250.00,
  "discountedPrice": 212.50,
  "loyaltyPrice": null,
  "discountPercentage": 15.00,
  "loyaltyDiscountPercentage": null
}
```
Display: ~~$250.00~~ → **$212.50** | Badge: "-15%"

---

### Scenario D: 15% ERP discount + PLATINUM user (15% loyalty)
**Product: Tailored Business Suit ($250)**

```json
{
  "originalPrice": 250.00,
  "discountedPrice": 212.50,
  "loyaltyPrice": 180.63,
  "discountPercentage": 15.00,
  "loyaltyDiscountPercentage": 15.00
}
```
Display: ~~$250.00~~ ~~$212.50~~ → **$180.63** | Badges: "-15%" + "Extra 15% Loyalty"

---

### Scenario E: 15% ERP discount + TITANIUM user (20% loyalty)
**Product: Tailored Business Suit ($250)**

```json
{
  "originalPrice": 250.00,
  "discountedPrice": 212.50,
  "loyaltyPrice": 170.00,
  "discountPercentage": 15.00,
  "loyaltyDiscountPercentage": 20.00
}
```
Display: ~~$250.00~~ ~~$212.50~~ → **$170.00** | Badges: "-15%" + "Extra 20% Loyalty"

---

### Scenario F: Expired discount, anonymous user
**Product: Classic Canvas Sneakers ($70)**
Backend filters out expired discount — returns as if no discount exists.

```json
{
  "originalPrice": 70.00,
  "discountedPrice": null,
  "loyaltyPrice": null,
  "discountPercentage": null,
  "loyaltyDiscountPercentage": null
}
```
Display: **$70.00** (no strikethrough, no badge)

---

### Scenario G: 25% time-limited ERP discount + GOLD user (5% loyalty)
**Product: Classic Polo Shirt ($50), sale ends in 14 days**

```json
{
  "originalPrice": 50.00,
  "discountedPrice": 37.50,
  "loyaltyPrice": 35.63,
  "discountPercentage": 25.00,
  "loyaltyDiscountPercentage": 5.00
}
```
Display: ~~$50.00~~ ~~$37.50~~ → **$35.63** | Badges: "-25%" + "Extra 5% Loyalty"

The SKU-level `discountedEndsAt` field tells you when the sale ends, if you want to show a countdown.

---

## 8. Seed Data Test Matrix

The dev seed data (`V17_1__seed_discount_scenarios.sql`) sets up these groups:

| Products | Template Codes | ERP Discount | `discountPercentage` | Expiry | Test Case |
|----------|---------------|-------------|---------------------|--------|-----------|
| 1–6 | TPL-TSHIRT, TPL-HOODIE, TPL-JEANS, TPL-WINDBRK, TPL-SKIRT, TPL-DRESS | **None** | `null` | — | Full price only. Loyalty discount visible for logged-in users. |
| 7–12 | TPL-SUIT, TPL-SWEATER, TPL-CARDGN, TPL-BLAZER, TPL-TRENCH, TPL-SHORTS | **15% off** | `15.00` | Never expires | ERP discount always active. All 3 prices + both percentages visible for tier users. |
| 13–18 | TPL-SWIM, TPL-PAJAMA, TPL-POLO, TPL-LINEN, TPL-JOGGER, TPL-FLEECE | **25% off** | `25.00` | +14 days | Time-limited sale. Shows all 3 prices + both percentages + expiry countdown. |
| 19–22 | TPL-TIE, TPL-SCARF, TPL-SNEAK, TPL-BOOTS | ~~30% off~~ | `null` (expired) | **EXPIRED** (−3 days) | `discountedPrice` and `discountPercentage` = null in API. Verifies expiry filtering. |
| 23–26 | TPL-SANDAL, TPL-CAP, TPL-BKPCK, TPL-WALLET | **10% off** | `10.00` | Never expires | Mild discount. |
| 27–30 | TPL-BELT, TPL-SUNGLS, TPL-WATCH, TPL-TOTE | **None** | `null` | — | Same as Group 1. |

### Test Users

| Phone | Role | Tier | Discount | Use for testing |
|-------|------|------|----------|-----------------|
| +992901234567 | USER | GOLD | 5% | Mild loyalty discount |
| +992902345678 | MANAGER | PLATINUM | 15% | Medium loyalty discount |
| +992903456789 | SYSTEM | PLATINUM | 15% | Same as above (different role) |
| +992904567890 | USER | TITANIUM | 20% | Maximum loyalty discount |
| (anonymous) | — | — | 0% | No loyalty price at all |

### Recommended Test Flow

1. Open `/api/v1/listings` **without auth** → verify `loyaltyPrice` is `null` everywhere
2. Open `/api/v1/listings` **as GOLD user** → verify `loyaltyPrice` appears, is 5% off base
3. Check product from Group 4 (e.g. slug containing `tpl-tie`) → `discountedPrice` should be `null` (expired)
4. Check product from Group 2 (e.g. slug containing `tpl-suit`) → all 3 prices present
5. Open `/api/v1/home/collections` → verify `on_sale` section only contains products with active (non-expired) discounts
6. Switch to TITANIUM user → verify `loyaltyPrice` is 20% off base

---

## 9. Authentication & How Loyalty Price Appears

Loyalty pricing is resolved **automatically by the backend** based on the JWT in the request:

```
Request with JWT cookie or Authorization header
    │
    ▼
Backend extracts userId from JWT
    │
    ▼
Loads user → gets tier → gets discountPercentage
    │
    ▼
Applies withLoyaltyPrice(discountPercentage) to every DTO
    │
    ▼
Response includes loyaltyPrice fields
```

**The frontend does NOT need to:**
- Fetch the user's discount percentage separately
- Calculate loyalty prices client-side
- Check discount expiry dates (backend handles it)
- Handle different tier logic

**The frontend SHOULD:**
- Pass the JWT on every request (cookie or header)
- Check if `loyaltyPrice !== null` to decide whether to show loyalty pricing
- Check if `discountedPrice !== null` to decide whether to show sale pricing

---

## 10. Loyalty Tiers Reference

Fetch from `GET /api/v1/loyalty-tiers` (public endpoint):

| Tier | Discount | Cashback | Min Spend | Color | Display Order |
|------|----------|----------|-----------|-------|---------------|
| TITANIUM | 20% | 10% | 100,000 | `#2D2D2D` | 1 (highest) |
| PLATINUM | 15% | 7.5% | 50,000 | `#7C8EA0` | 2 |
| GOLD | 5% | 2.5% | 10,000 | `#C8962D` | 3 (lowest) |

`displayOrder` 1 = most prestigious. Sort ascending for "best to worst" display.

---

## 11. Frontend Display Recommendations

### Product Card (Grid)

```
┌─────────────────────────┐
│  [Image]                │
│                         │
│  Product Name           │
│  Color: Midnight Black  │
│                         │
│  ~~$250.00~~            │  ← originalPrice (strikethrough if any discount)
│  ~~$212.50~~            │  ← discountedPrice (strikethrough if loyaltyPrice exists)
│  $180.63                │  ← loyaltyPrice (bold, highlighted)
│  ┌──────────────────┐   │
│  │ 🏷 PLATINUM -15% │   │  ← tier badge (optional)
│  └──────────────────┘   │
└─────────────────────────┘
```

### Price Tag Component Logic

```typescript
function PriceDisplay({
  originalPrice, discountedPrice, loyaltyPrice,
  discountPercentage, loyaltyDiscountPercentage
}: ListingVariantDto) {
  const finalPrice = loyaltyPrice ?? discountedPrice ?? originalPrice;
  const hasAnyDiscount = discountedPrice != null || loyaltyPrice != null;

  return (
    <div>
      {hasAnyDiscount && (
        <span className="line-through text-gray-400">
          ${originalPrice}
        </span>
      )}

      {discountedPrice != null && loyaltyPrice != null && (
        <span className="line-through text-gray-400 text-sm">
          ${discountedPrice}
        </span>
      )}

      <span className="font-bold text-lg">
        ${finalPrice}
      </span>

      {/* Use backend-provided percentages — no client-side calculation */}
      {discountPercentage != null && (
        <span className="bg-red-100 text-red-600 text-xs px-1 rounded">
          -{discountPercentage}%
        </span>
      )}

      {loyaltyDiscountPercentage != null && (
        <span className="bg-amber-100 text-amber-700 text-xs px-1 rounded">
          Extra {loyaltyDiscountPercentage}% loyalty
        </span>
      )}
    </div>
  );
}
```

### SKU Selector (Detail Page)

Each size has its own prices. When the user selects a size, show that SKU's prices:

```typescript
function SkuPrice({ sku }: { sku: SkuDto }) {
  const finalPrice = sku.loyaltyPrice ?? sku.discountedPrice ?? sku.originalPrice;

  return (
    <div>
      {sku.onSale && <span className="line-through">${sku.originalPrice}</span>}
      {sku.loyaltyPrice && sku.discountedPrice && (
        <span className="line-through text-sm">${sku.discountedPrice}</span>
      )}
      <span className="font-bold">${finalPrice}</span>
      {sku.onSale && sku.discountedEndsAt && (
        <Countdown endsAt={sku.discountedEndsAt} />
      )}
    </div>
  );
}
```

---

## 12. Migration Notes (Breaking Changes from Old API)

### Fields Renamed

| Old Field | New Field | Notes |
|-----------|-----------|-------|
| `priceStart` | `originalPrice` | Was min price across SKUs |
| `priceEnd` | *(removed)* | Prices are uniform per variant |
| `tierPriceStart` | `loyaltyPrice` | Renamed for clarity |
| `tierPriceEnd` | *(removed)* | Single value now |
| — | `discountedPrice` | **New field** — ERP product discount |
| — | `discountPercentage` | **New field** — ERP discount % (e.g. `15.00`) |
| — | `loyaltyDiscountPercentage` | **New field** — User's loyalty tier discount % |

### Fields Removed

- `originalPriceStart` / `originalPriceEnd` → replaced by single `originalPrice`
- `discountedPriceStart` / `discountedPriceEnd` → replaced by single `discountedPrice`
- `loyaltyPriceStart` / `loyaltyPriceEnd` → replaced by single `loyaltyPrice`

### SkuDto Changes

| Old Field | New Field |
|-----------|-----------|
| `price` | `originalPrice` |
| `salePrice` | `discountedPrice` |
| `tierPrice` | `loyaltyPrice` |
| `saleEndsAt` | `discountedEndsAt` |

### ListingIndexPort (ES)

- `priceStart` / `priceEnd` → single `price` (lowest effective price for search ranking)

### Key Behavior Change

**Expired discounts are now filtered server-side.** If `discounted_ends_at` is in the past, the API returns `discountedPrice: null` and `onSale: false`. The frontend no longer needs to check expiry timestamps to decide whether to show a sale price.
