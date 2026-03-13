# Backend Uncommitted Changes — Review Report

**Date:** 2026-03-14  
**Branch:** `improve/loyalty_card`  
**Scope:** All uncommitted changes after loyalty tier implementation + Phase 5 review fixes

---

## Summary

The uncommitted changes cover:
- `rank` → `displayOrder` rename across ~10 files
- New `TierPricingEnricher.java` for server-side tier discount computation on product DTOs
- `tierPrice` / `tierPriceStart` / `tierPriceEnd` fields added to `SkuDto`, `ListingVariantDto`, `ListingVariantDetailDto`
- `computeSpendToNextTier` added to both `SyncUsersService` and `SyncLoyaltyPointsService`
- BigDecimal Jackson serialization config in `application.yml`
- `V15__rename_rank_to_display_order.sql` migration

---

## 🐛 Bug: `computeSpendToNextTier` picks the WRONG tier

**Files:** `SyncLoyaltyPointsService.java` (line 84), `SyncUsersService.java` (line 133)

Both services contain identical logic:
```java
var tiers = loadLoyaltyTierPort.findAll();          // returns sorted by displayOrder ASC
LoyaltyTier lowestTier = tiers.get(tiers.size() - 1); // picks the LAST element
```

`findAll()` delegates to `findAllByOrderByDisplayOrderAsc()`, which means:
- `tiers.get(0)` = lowest `displayOrder` = **the entry-level tier** (e.g., Gold, rank 1)
- `tiers.get(tiers.size() - 1)` = highest `displayOrder` = **the top-tier** (e.g., Titanium, rank 3)

The intent is to compute the gap to the *first/entry-level* tier for users with no tier. But the code grabs the **last** element — the most expensive tier.

### Fix

```diff
- LoyaltyTier lowestTier = tiers.get(tiers.size() - 1);
+ LoyaltyTier lowestTier = tiers.get(0);
```

Apply this fix in **both** `SyncLoyaltyPointsService.java` and `SyncUsersService.java`.

---

## ⚠️ Minor Observations (Non-blocking)

### 1. Duplicated `computeSpendToNextTier` logic

The exact same method is copy-pasted in both `SyncLoyaltyPointsService` and `SyncUsersService`. Consider extracting it into a shared domain service or utility to avoid future drift between the two copies.

### 2. `TierPricingEnricher` loads user on every request

`resolveDiscount()` calls `loadUserPort.loadById(principal.userId())` on every product endpoint invocation. For high-traffic pages (home, listings, search, category), this adds one extra DB query per request. This is acceptable for now but worth noting for future optimization (e.g., caching the discount in the JWT claims or in a request-scoped cache).

### 3. `ListingVariantDto.withTierDiscount` applies discount to base price, not sale price

The `ListingVariantDto` computes `tierPriceStart` and `tierPriceEnd` from `priceStart` / `priceEnd` (which are base prices). However, `SkuDto.withTierDiscount` correctly uses `salePrice != null ? salePrice : price` as the effective base. This means the listing grid card (`ListingVariantDto`) could show a tier price that doesn't account for active sales, while the detail page (`SkuDto`) does. Verify this is intentional or align the behavior.

### 4. Jackson `write-bigdecimal-as-plain` may not work via `spring.jackson.generator`

The config added is:
```yaml
spring:
  jackson:
    generator:
      write-bigdecimal-as-plain: true
```

Spring Boot maps `spring.jackson.serialization.*` and `spring.jackson.deserialization.*` properties directly, but `generator` properties may need to be set via a custom `Jackson2ObjectMapperBuilderCustomizer` bean instead. Verify this actually takes effect by testing a response containing a `BigDecimal` value like `0.00` and confirming it doesn't serialize as `"0.00"` (string) or `0E+2` (scientific notation).

### 5. No null-check on `user.loyalty()` in `SyncLoyaltyPointsService`

Line 45: `LoyaltyTier tier = resolveTier(command.tierName(), user.loyalty().tier());`

If `user.loyalty()` returns `null` (e.g., pre-existing users before the migration), this will throw a `NullPointerException`. The `LoyaltyProfile.empty()` factory was designed for exactly this case. Confirm the `UserMapper` always produces a non-null `LoyaltyProfile` when mapping from `UserEntity`, or add a defensive check.

---

## ✅ What Looks Good

| Area | Verdict |
|------|---------|
| `rank` → `displayOrder` rename consistency | All 10+ files updated correctly |
| `V15` migration | Clean ALTER + index rename |
| `TierPricingEnricher` architecture | Clean Spring component, proper security context resolution |
| `withTierDiscount` on DTOs | Immutable record pattern, correct rounding |
| `null` passthrough for `tierPrice*` in adapters | Correctly passes `null` for controller enrichment |
| BigDecimal rounding (`HALF_UP`, scale 2) | Consistent across all DTO methods |
