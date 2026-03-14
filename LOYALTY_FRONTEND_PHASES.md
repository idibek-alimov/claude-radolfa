# Loyalty Tiers — Frontend Implementation Phases

> **Reference:** `FRONTEND_LOYALTY_INTEGRATION.md`
> **Started:** 2026-03-14

---

## Phase 1: Types & Data Layer
Update TypeScript types and data-fetching infrastructure.

- [x] Create `entities/loyalty/model/types.ts` — `LoyaltyTier`, `LoyaltyProfile`
- [x] Create `entities/loyalty/api.ts` — `useLoyaltyTiers()` query hook
- [x] Create `entities/loyalty/index.ts` barrel export
- [x] Update `entities/user/model/types.ts` — replace `loyaltyPoints` with `loyalty: LoyaltyProfile`
- [x] Update `features/user-management/types.ts` — same change for admin `UserDto`
- [x] Update `entities/product/model/types.ts` — add `tierPriceStart`, `tierPriceEnd`, `tierPrice`
- [x] Update `useAuth()` — handles new shape (types flow through `User`)
- [x] Add `refreshUser()` to `useAuth()` for post-purchase re-fetch

**Status:** Complete

---

## Phase 2: Fix All `loyaltyPoints` References
Find and migrate every usage of the old flat field.

- [x] Profile page (`app/(storefront)/profile/page.tsx`) — removed hardcoded TIERS/getCurrentTier, now uses `user.loyalty.*`
- [x] Navbar / user dropdown (points badge) — `user.loyalty.points`
- [x] User management table — `user.loyalty.points`
- [x] i18n locales (en/ru/tj) — removed old tier keys, added `spendToNextTier`, `startEarning`, `discount`
- [x] Build passes with zero type errors

**Status:** Complete

---

## Phase 3: Tier Pricing Display
Show tier-discounted prices on product listings and detail pages.

- [x] ProductCard — strikethrough base price + bold tier price when `tierPriceStart` present
- [x] ProductDetail — strikethrough original range above tier-discounted range
- [x] `useAuth` — invalidate `["listings"]` and `["home-collections"]` on successful login (logout already does `queryClient.clear()`)
- [x] Price helpers not needed — inline logic is simpler for 2 components
- [x] Build passes with zero type errors

**Status:** Complete

---

## Phase 4: Loyalty UI Components
Build the loyalty-specific UI components (FSD features layer).

- [x] `features/loyalty/ui/LoyaltyCard.tsx` — gradient card with points, tier name, discount %, cashback %
- [x] `features/loyalty/ui/TierProgress.tsx` — progress bar + spend-to-next + demotion warning (3 severity levels)
- [x] `features/loyalty/ui/TiersList.tsx` — all tiers from API, highlights current tier, shows discount/cashback/min-spend
- [x] `features/loyalty/index.ts` — barrel export
- [x] i18n keys added (en/ru/tj): cashback, minSpend, allTiers, yourTier, demotionWarning
- [x] Build passes with zero type errors

**Status:** Complete

---

## Phase 5: Widget Integration & Profile Page
Compose components into widgets and wire into pages.

- [x] `widgets/loyalty-dashboard/LoyaltyDashboard.tsx` — composes LoyaltyCard + TierProgress + TiersList + "How it works"
- [x] Profile page loyalty tab replaced with `<LoyaltyDashboard />` (~75 lines → 1 line)
- [x] Removed unused variables (`tierName`, `spendToNext`, `tierProgress`, etc.) and `AlertCircle` import
- [x] Navbar desktop dropdown + mobile menu — show tier name badge alongside points
- [x] Build passes with zero type errors

**Status:** Complete
