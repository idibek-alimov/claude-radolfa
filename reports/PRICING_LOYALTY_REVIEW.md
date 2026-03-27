# Pricing & Loyalty Program — Code Review

> Files reviewed: `TierPricingEnricher`, `ListingVariantDto`, `ListingVariantDetailDto`, `SkuDto`,
> `ListingReadAdapter`, `ResolveUserDiscountService`, `LoyaltyController`, `ListingController`,
> `HomeController`, `LoyaltyProfile`, `LoyaltyTier`, `ResolveUserDiscountUseCase`, `PRICING_API_GUIDE.md`

> **Status**: Reviewed 2026-03-15 — 3 bugs fixed, 6 issues still pending

---

## 🐛 Logic Errors / Bugs

### 1. `TierPricingEnricher` makes N+1 DB calls for every enrich

**Status**: ⚠️ **NOT FIXED**

**Location**: `TierPricingEnricher.resolveDiscount()` — called separately inside each of the four `enrich(…)` overloads.

**Problem**: For grid/collection endpoints each of the four `enrich*()` methods calls `resolveDiscount()` which ultimately hits `resolveUserDiscountUseCase.resolveForUser(userId)` → DB query.
A single HTTP request to `/api/v1/home/collections` triggers this DB query **once per method invocation**, but if any code path ever calls two enrich methods in one request (or the same method twice) the user is loaded from DB multiple times unnecessarily.

More importantly, for paged results each `PageResult` could contain 12–24 items, all enriched in a loop — the discount is resolved once per *enrich call* (not per item), which is fine, but the user row is loaded every single request instead of being cached.

**Suggestion**: Cache the resolved discount in a `@RequestScope` Spring bean, or eagerly read it once from the JWT (if the discount % is embedded in the token), or at minimum memoize the result in the `TierPricingEnricher` per-call:

```java
// Simple per-call memo — zero extra DB hit for subsequent enrichXxx() calls in the same thread
public BigDecimal resolveDiscount() {
    // ...existing logic...
}
// Controllers already call enrich once per request, so this is mostly
// future-proofing, but important if HomeController were ever refactored.
```

The cleanest fix: embed the resolved discount in a `@RequestScope` component.

---

### 2. `isDiscountActive` in `ListingReadAdapter` duplicated & inconsistent with JPQL query

**Status**: ⚠️ **NOT FIXED** (JPQL now has the check, but logic is still duplicated)

**Location**: `ListingReadAdapter.isDiscountActive()` (line 209–213) and the grid JPQL query.

**Problem**: The grid path uses a JPQL `WHERE` clause (in `findGridPage`) to expiry-filter discounts at the DB level. The detail path uses the in-memory `isDiscountActive()` helper — which re-checks `discountedPrice < originalPrice` in addition to the expiry check.

This means:
- **Grid**: expiry-filtered by SQL; no sanity check that `discountedPrice < originalPrice`.
- **Detail + SKU**: expiry-filtered AND validates `discountedPrice < originalPrice`.

If ERP ever sends a `discountedPrice >= originalPrice` (a data error), the grid would still show it as a sale while the detail page would not. Inconsistency.

**Suggestion**: Centralise the active-discount predicate. Either add the `discountedPrice < originalPrice` check to the JPQL, or apply `isDiscountActive` as a post-filter on grid rows too (the grid currently just trusts whatever the DB returns).

---

### 3. `discountPercentage` can be `MIN` on the detail page while `discountedPrice` is also `MIN` — they may come from different SKUs

**Status**: ✅ **FIXED** — Both values now derived from the same SKU

**Location**: `ListingReadAdapter.toDetailDto()` lines 137–148.

**Original Problem**:
```java
BigDecimal discountedPrice = skuEntities.stream()
        .filter(s -> isDiscountActive(s, now))
        .map(SkuEntity::getDiscountedPrice)
        .min(BigDecimal::compareTo)
        .orElse(null);

BigDecimal discountPercentage = skuEntities.stream()
        .filter(s -> isDiscountActive(s, now))
        .map(SkuEntity::getDiscountPercentage)
        .filter(Objects::nonNull)
        .min(BigDecimal::compareTo)
        .orElse(null);
```

These are two **independent** `min()` operations. If SKU-A has `discountedPrice=100, discountPct=30` and SKU-B has `discountedPrice=120, discountPct=10`, the result will be:
- `discountedPrice = 100` (from SKU-A)
- `discountPercentage = 10` (from SKU-B)

…which is internally inconsistent — the shown `discountPercentage` does **not** match the shown `discountedPrice`.

**Fix Applied**:
```java
SkuEntity cheapestDiscounted = skuEntities.stream()
        .filter(s -> isDiscountActive(s, now))
        .min(Comparator.comparing(SkuEntity::getDiscountedPrice))
        .orElse(null);

BigDecimal discountedPrice = cheapestDiscounted != null
        ? cheapestDiscounted.getDiscountedPrice() : null;
BigDecimal discountPercentage = cheapestDiscounted != null
        ? cheapestDiscounted.getDiscountPercentage() : null;
```

> [!IMPORTANT]
> This was a **data-correctness bug** observable by users: the badge percentage (e.g. "-10%") wouldn't match the displayed price (which is the 30%-off price). It will manifest whenever a product has SKUs with different discount percentages.

---

### 4. `toSkuDto` calls `Instant.now()` per SKU — small but avoidable

**Status**: ✅ **FIXED** — `now` is now captured before the stream

**Location**: `ListingReadAdapter.toSkuDto()` line 226.

**Original Problem**: `Instant.now()` is called once per SKU inside the stream loop. For a product with many sizes this creates minor inconsistency (the `now` reference point drifts slightly across SKUs in the same request, though in practice < 1 ms).

**Fix Applied**:
```java
// In toDetailDto():
Instant now = Instant.now();  // Captured once before stream
List<SkuDto> skus = skuEntities.stream()
        .map(sku -> toSkuDto(sku, now))   // now passed as parameter
        .toList();
```

---

### 5. `withLoyaltyPrice` guard in `ListingVariantDto` has a redundant null-check

**Status**: ⚠️ **NOT FIXED**

**Location**: `ListingVariantDto.withLoyaltyPrice()` lines 41–42.

```java
BigDecimal lp = base != null
        ? base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP) : null;
```

`base` is either `discountedPrice` (already checked non-null above) or `originalPrice`. The `originalPrice` field is documented as "Never null for products with SKUs". So the `base != null` guard is defensive dead-code in practice — and silently produces `loyaltyPrice = null` if originalPrice is somehow null, which would be confusing. Either document the assumption or throw early.

---

## 💡 Improvement Suggestions

### A. Loyalty discount is re-fetched per HTTP request — consider embedding it in JWT

**Status**: ⚠️ **NOT FIXED**

The `ResolveUserDiscountService` does a DB lookup on every single request that touches any listing endpoint. Since the discount% only changes when a user's tier changes (rare), it would be more efficient to:
- Embed `discountPercentage` in the JWT at login/refresh time, **or**
- Add a short-lived `@RequestScope` cache in `TierPricingEnricher`.

This eliminates a DB round-trip for the by far most-frequently-called endpoints.

---

### B. `LoyaltyController.updateColor` has no validation on the `color` field

**Status**: ⚠️ **NOT FIXED**

**Location**: `LoyaltyController.updateTierColor()`, `UpdateColorRequest`.

The hex code is accepted as a raw `String` with zero validation. A manager could store `" "`, `"yellow"`, or malformed values that break the frontend color parser.

**Suggestion**: Add a `@Pattern` constraint:
```java
record UpdateColorRequest(
    @NotBlank
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Must be a valid hex color (e.g. #C8962D)")
    String color
) {}
```

---

### C. `ResolveUserDiscountService`: user not found is silently treated as "no discount"

**Status**: ⚠️ **NOT FIXED**

If `loadUserPort.loadById(userId)` returns `Optional.empty()` (e.g. deleted user with a still-valid JWT), the service returns `BigDecimal.ZERO`, which means the client gets no loyalty pricing. This is correct behaviour but it's completely silent — no log, no metric. If a JWT is issued for a user who gets deleted, all pricing calls silently degrade without any trace.

**Suggestion**: Add a `log.warn(...)` when the user isn't found so the anomaly is observable.

---

### D. `ListingReadAdapter.toSkuDto` — `onSale` flag not consistent with `discountedEndsAt`

**Status**: ✅ **FIXED** — `discountedEndsAt` now nulled when `onSale = false`

The DTO exposes `discountedEndsAt` (expiry timestamp) even when `onSale = false` (meaning the discount is expired). The frontend could still read a past timestamp and potentially render a "sale ended X days ago" countdown.

The API guide says the frontend should **not** check expiry, but sending the raw `discountedEndsAt` when the discount is inactive is misleading and a potential source of frontend misuse.

**Fix Applied**:
```java
return new SkuDto(
        ...
        onSale,
        onSale ? entity.getDiscountedEndsAt() : null  // ← null when not on sale
);
```

---

## ✅ What's Done Well

- **Clean separation of concerns**: loyalty price enrichment is completely isolated in `TierPricingEnricher`, DTOs are all immutable records with a pure `withLoyaltyPrice()` method — easy to test.
- **Expiry filtering is centralised**: grid path filters at SQL level, detail path filters in `isDiscountActive()` — the contract ("frontend never checks expiry") holds everywhere.
- **`BigDecimal` throughout**: no floating-point math for prices, correct `HALF_UP` rounding, consistent 2/4 decimal place scales.
- **`loyaltyPrice` always applied on top of the effective base** (`discountedPrice ?? originalPrice`) — the stacking formula is correctly implemented.
- **Null propagation is safe**: if originalPrice is null the enricher returns null loyalty price rather than throwing.
