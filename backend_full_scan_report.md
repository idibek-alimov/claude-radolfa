# Full Backend Scan — Loyalty Tiers Implementation

**Date:** 2026-03-14
**Branch:** `improve/loyalty_card`

---

## Purpose Understood ✅

The core goal is to **upgrade the loyalty system from a single-tier integer-based points system to a multi-tier loyalty framework** where:

1. **ERPNext defines tiers** (Gold, Platinum, Titanium, etc.) with specific discount/cashback percentages and spending thresholds
2. **ERPNext syncs per-user tier assignments**, spending progress, and promotion/demotion data
3. **The backend stores and serves** tier definitions + per-user loyalty profiles
4. **The frontend calculates discounted prices** using the user's tier discount percentage applied to base prices
5. **The backend ALSO now provides** server-computed tier prices via `TierPricingEnricher` (added in Phase 5 review fixes)

---

## Layer-by-Layer Verification

### 1. Database Migrations ✅

| Migration | Purpose | Status |
|-----------|---------|--------|
| `V14__loyalty_tiers.sql` | Creates `loyalty_tiers` table + adds `tier_id`, spend columns to `users` | Correct — all columns match entity fields |
| `V15__rename_rank_to_display_order.sql` | Renames `rank` → `display_order` + index rename | Correct — clean ALTER |

### 2. Domain Layer ✅

| File | Status | Notes |
|------|--------|-------|
| `LoyaltyTier.java` | ✅ | Record with `id, name, discountPercentage, cashbackPercentage, minSpendRequirement, displayOrder, version` |
| `LoyaltyProfile.java` | ✅ | Value Object with `tier, points, spendToNextTier, spendToMaintainTier, currentMonthSpending` + `empty()` factory |
| `User.java` | ✅ | Has `LoyaltyProfile loyalty` field replacing old `int loyaltyPoints` |

### 3. Entity Layer ✅

| File | Status | Notes |
|------|--------|-------|
| `LoyaltyTierEntity.java` | ✅ | Maps to `loyalty_tiers`, has `displayOrder` column |
| `UserEntity.java` | ✅ | Has `@ManyToOne tier`, `spendToNextTier`, `spendToMaintainTier`, `currentMonthSpending`. Retains `loyaltyPoints` int column for DB backward compat |

### 4. Hexagonal Ports ✅

| File | Status | Notes |
|------|--------|-------|
| `LoadLoyaltyTierPort.java` | ✅ | `findByName()`, `findAll()` |
| `SaveLoyaltyTierPort.java` | ✅ | `save()`, `saveAll()` |

### 5. Adapters & Repositories ✅

| File | Status | Notes |
|------|--------|-------|
| `LoyaltyTierRepository.java` | ✅ | `findByName()`, `findAllByOrderByDisplayOrderAsc()` |
| `LoyaltyTierRepositoryAdapter.java` | ✅ | Implements both ports, uses mapper correctly |

### 6. Mappers ✅

| File | Status | Notes |
|------|--------|-------|
| `LoyaltyTierMapper.java` | ✅ | MapStruct `toDomain()` / `toEntity()` with audit field ignores |
| `UserMapper.java` | ✅ | Bridges flat `UserEntity` fields → nested `LoyaltyProfile` via `@Named("toLoyaltyProfile")`. Handles null tier gracefully |

### 7. Sync Use Cases & Services ✅

| File | Status | Notes |
|------|--------|-------|
| `SyncLoyaltyTiersUseCase.java` | ✅ | `SyncTierCommand` with `displayOrder` |
| `SyncLoyaltyTiersService.java` | ✅ | Upserts by name, preserves `id`/`version` on update |
| `SyncUsersUseCase.java` | ✅ | `SyncUserCommand` has `tierName`, spend fields |
| `SyncUsersService.java` | ✅ | Resolves tier by name, builds `LoyaltyProfile`, null-safe with `LoyaltyProfile.empty()` |
| `SyncLoyaltyPointsUseCase.java` | ✅ | `SyncLoyaltyCommand` has tier + spend fields |
| `SyncLoyaltyPointsService.java` | ✅ | Same pattern, null-safe, `computeSpendToNextTier` with `tiers.get(0)` (fixed) |

### 8. ErpSyncController ✅

| Endpoint | Status | Notes |
|----------|--------|-------|
| `POST /sync/loyalty-tiers` | ✅ | Idempotency-Key required, EVENT_LOYALTY_TIER, maps `SyncLoyaltyTierPayload` → `SyncTierCommand` |
| `POST /sync/loyalty` | ✅ | Passes all tier+spend fields to `SyncLoyaltyCommand` |
| `POST /sync/users` | ✅ | Passes all tier+spend fields to `SyncUserCommand` |
| `POST /sync/users/batch` | ✅ | Same via `toUserCommand()` helper |

### 9. DTOs ✅

| File | Status | Notes |
|------|--------|-------|
| `SyncLoyaltyTierPayload.java` | ✅ | Dedicated payload: `name, discountPercentage, cashbackPercentage, minSpendRequirement, displayOrder` |
| `SyncLoyaltyRequestDto.java` | ✅ | Expanded with `tierName`, spend fields |
| `SyncUserPayload.java` | ✅ | Expanded with `tierName`, spend fields |
| `LoyaltyTierDto.java` | ✅ | `fromDomain()` factory, has `displayOrder` |
| `UserDto.java` | ✅ | Nested `LoyaltyDto` with `LoyaltyTierDto` — matches plan's JSON structure. Null-safe `fromDomain()` |
| `SkuDto.java` | ✅ | Added `tierPrice` + `withTierDiscount()` |
| `ListingVariantDto.java` | ✅ | Added `tierPriceStart/End` + `withTierDiscount()` |
| `ListingVariantDetailDto.java` | ✅ | Added `tierPriceStart/End` + `withTierDiscount()` enriching SKUs too |

### 10. TierPricingEnricher (Phase 5 Addition) ✅

| Aspect | Status | Notes |
|--------|--------|-------|
| Resolves discount from security context | ✅ | Uses `LoadUserPort.loadById()` → `User.loyalty().tier().discountPercentage()` |
| Enriches `PageResult<ListingVariantDto>` | ✅ | |
| Enriches `ListingVariantDetailDto` | ✅ | Also enriches SKUs within the detail |
| Enriches `HomeSectionDto` list | ✅ | |
| Enriches `CollectionPageDto` | ✅ | |
| Returns input unchanged for 0% discount | ✅ | Short-circuits correctly |

### 11. Controllers Using TierPricingEnricher ✅

| Controller | Status |
|-----------|--------|
| `ListingController` (GET /listings, GET /listings/{slug}, GET /listings/search) | ✅ Enriched |
| `HomeController` (GET /collections, GET /collections/{key}) | ✅ Enriched |
| `CategoryController` (GET /categories/{id}/listings) | ✅ Enriched |

### 12. AuthController — `/api/v1/auth/me` ✅

- `GET /me` returns `UserDto.fromDomain(user)` which now includes the nested `LoyaltyDto` with tier details and spend progress
- Correctly accesses the security context and loads user by ID

### 13. LoyaltyController — `GET /api/v1/loyalty-tiers` ✅

- Returns all tiers ordered by `displayOrder` ASC via `LoadLoyaltyTierPort.findAll()`
- Maps to `LoyaltyTierDto`

### 14. SecurityConfig ✅

- `/api/v1/sync/**` → SYSTEM role only (covers `/sync/loyalty-tiers`)
- `/api/v1/auth/**` → `permitAll()` (covers `/auth/me` for authenticated users — the JWT filter still runs)
- `GET /api/v1/loyalty-tiers` → falls through to `.anyRequest().authenticated()` — **this means only logged-in users can see tiers**. This is probably correct since tier info is only useful when showing user-specific progress.

---

## Verdict: Everything Fits Together ✅

The implementation is **complete and consistent** across all layers. The data flows correctly:

```
ERPNext → /sync/loyalty-tiers → SyncLoyaltyTiersService → loyalty_tiers table
ERPNext → /sync/loyalty       → SyncLoyaltyPointsService → users table (tier_id, spend fields)
ERPNext → /sync/users          → SyncUsersService          → users table (tier_id, spend fields)

Frontend → GET /auth/me         → UserDto { loyalty: { tier, points, spend* } }
Frontend → GET /loyalty-tiers   → List<LoyaltyTierDto> (all tiers for progress bars)
Frontend → GET /listings/*      → ListingVariantDto { tierPriceStart, tierPriceEnd } (server-computed)
```

No missing files, no broken references, no type mismatches. The hexagonal architecture is properly maintained throughout.
