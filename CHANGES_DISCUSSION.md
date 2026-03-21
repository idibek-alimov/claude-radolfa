# Radolfa — Planned Changes Discussion

> This file is a working document. We list what needs to change, discuss decisions,
> then convert each item into a concrete implementation plan/phases.

---

## 1. Pricing Display — `ListingVariantDto` & `ListingVariantDetailDto`

**Status:** All decisions made ✓. Ready for implementation planning.

**Goal:** Product cards and detail pages should show rich pricing info:
- Original price (strikethrough when a cheaper price exists)
- Sale discounted price + discount % badge + discount name + color
- Loyalty "Your Price" (for authenticated users with a tier only)
- Loyalty % badge (for authenticated users with a tier only)

---

### Settled Decisions ✓

**Field rename — free to rename everything:**
Old field names (`minPrice`, `maxPrice`, `tierDiscountedMinPrice`) can be replaced
entirely. No backwards-compat obligation.

**`originalPrice`** (replaces `maxPrice` / `minPrice` raw)
- The original (pre-discount) price of the cheapest SKU when comparing discounted prices.
- Always populated. When no discount is active it equals the effective price (no strikethrough needed).

**`discountPrice`** (replaces `minPrice` effective)
- The sale-discounted price. `null` when no active discount exists for this variant.

**`discountPercentage`, `discountName`, `discountColorHex`**
- All included. Already computed inside `DiscountEnrichmentAdapter.DiscountInfo`; just need wiring into the DTO.
- `null` when no active discount.

**`loyaltyPrice` ("Your Price") and `loyaltyPercentage`**
- Shown **only** to authenticated users who have a loyalty tier. `null` for guests and
  users without a tier. No marketing teaser for guests.

**Visibility matrix:**

| Field | Guest | Auth (no tier) | Auth (with tier) |
|-------|-------|----------------|------------------|
| `originalPrice` | ✓ | ✓ | ✓ |
| `discountPrice` | ✓ (if sale) | ✓ (if sale) | ✓ (if sale) |
| `discountPercentage` | ✓ (if sale) | ✓ (if sale) | ✓ (if sale) |
| `discountName` | ✓ (if sale) | ✓ (if sale) | ✓ (if sale) |
| `discountColorHex` | ✓ (if sale) | ✓ (if sale) | ✓ (if sale) |
| `loyaltyPrice` | ✗ | ✗ | ✓ |
| `loyaltyPercentage` | ✗ | ✗ | ✓ |

**Card vs. detail — same fields everywhere:**
Both product card and detail page expose the same set of fields.
Detail page additionally supports per-SKU price swap: when the user selects a size,
the frontend swaps to that `SkuDto`'s own `originalPrice` / `discountPrice` / `loyaltyPrice`.
`SkuDto` must therefore carry the same pricing fields (not just one `price`).

**No stacking — best-of is the rule (already implemented in backend logic):**
When a SKU has an active sale discount AND the user has a loyalty tier, the backend
picks whichever gives the lower price. They never multiply together.

---

### `loyaltyPrice` — Decision ✓

**Formula:**
```
loyaltyPrice = originalPrice × (1 − max(discountPercentage, loyaltyPercentage) / 100)
```
Discounts are stored as whole percentages (e.g. 20 means 20%, not 0.2).

- Takes whichever percentage is higher — sale or tier — and applies it once to `originalPrice`.
- No stacking. No double-discounting.
- Always shown to authenticated users who have a tier, regardless of which percentage wins.
  Reason: always displaying "Your Price" makes the card/detail more appealing and reinforces
  the value of having a loyalty tier.

**Worked examples (originalPrice = 1000, percentages as whole numbers):**

| Scenario | discountPct | loyaltyPct | max | loyaltyPrice |
|----------|-------------|------------|-----|--------------|
| No sale, has tier | 0 | 10 | 10 | 1000 × (1 − 10/100) = **900** |
| Sale beats tier | 15 | 5 | 15 | 1000 × (1 − 15/100) = **850** |
| Tier beats sale | 5 | 15 | 15 | 1000 × (1 − 15/100) = **850** |
| No sale, no tier | — | — | — | `null` |

**`loyaltyPercentage` in the DTO** = the user's own tier discount %, not the effective %.
The frontend shows the tier badge ("Gold 5%") independently of which % was actually applied.

---

### Open Question — `loyaltyPercentage` badge when sale wins

When `discountPercentage (15%) > loyaltyPercentage (5%)`:
- `loyaltyPrice = 850` (sale drove it, not tier)
- `loyaltyPercentage = 5%` (user's tier %)
- The badge would say "Gold 5%" but the actual saving is 15%

**Does this matter for the UI?** Two options:
- **A — Show tier % as-is.** Badge always says the tier benefit. User can infer the price
  is 850 because the sale happened to be better.
- **B — Show `effectivePercentage` = `max(discountPct, loyaltyPct)`.** Badge says "15% off"
  when sale wins, "5% off" when tier wins. More accurate but the badge now conflates
  two different mechanisms.

> **Decision ✓ — Option A.** `loyaltyPercentage` always reflects the user's own tier %, not
> the effective applied %. The badge describes the tier benefit; `loyaltyPrice` shows the
> result. They are independent and do not need to match.

---

### Partial & Mixed Discounts Across SKUs — Decision ✓

**Researched against Amazon, Noon, and Wildberries.** None of the three platforms show
a "From X" price range or a "select sizes on sale" indicator on the product card.
The universal industry pattern: card shows one representative price/badge; the detail
page reveals per-SKU truth when a size is selected.

Radolfa follows the same pattern.

**Definitions:**

- **Partial discount** — some SKUs of the variant have an active discount, some do not
  (e.g., XL and XXL on sale, M and L at full price).
- **Mixed discount** — different SKUs of the same variant have *different* active discount
  campaigns (e.g., XL → "Summer Sale 20%", XXL → "Clearance 30%").

**Card behavior (both cases):**
- Show the discount info (`discountPrice`, `discountPercentage`, `discountName`,
  `discountColorHex`) of the SKU that produces the **cheapest discounted price**.
  This is the existing `DiscountEnrichmentAdapter` behavior — no change needed.
- Add `isPartialDiscount: boolean` to the DTO. `true` when at least one SKU in the
  variant has no active discount (or a different discount campaign). Frontend can use
  this to optionally render a subtle "on select sizes" hint alongside the badge, but
  the main price and badge stand as-is.
- For mixed discounts specifically: the winning discount's `discountName` and
  `discountColorHex` are shown on the card. Other campaigns are invisible at card level.

**`isPartialDiscount` logic:**
```
isPartialDiscount = true
  if any SKU in the variant has no active discount
  OR if discounted SKUs do not all share the same discount campaign (discountId differs)
```

**Detail page (per-SKU truth):**
- Each `SkuDto` carries its own full pricing block:
  `originalPrice`, `discountPrice`, `discountPercentage`, `discountName`,
  `discountColorHex`, `loyaltyPrice` (for auth tier users).
- When the user selects a size, the frontend swaps to that SKU's pricing.
- A non-discounted size → `discountPrice = null`, discount badge disappears.
- A differently-discounted size → its own campaign name and color badge appear.

**`SkuDto` full field list (replaces current single `price` field):**
```
originalPrice        — always populated
discountPrice        — null if this SKU has no active discount
discountPercentage   — null if no discount
discountName         — null if no discount
discountColorHex     — null if no discount
loyaltyPrice         — null for guests/no-tier users; best-of formula applied per SKU
```

---

## 2. Loyalty Tier Assignment — Remove Automatic Tier on Registration

**Status:** Decisions made. Ready for implementation planning.

**Current behavior:** Every registered user automatically gets a loyalty profile with a tier
(Gold / Platinum / etc.). Tier is assigned at registration.

**Desired behavior:** Users register with NO tier. A loyalty profile is still created (with
`null` tier), and the UI shows a prompt like "Spend X to enter the tier system." Tiers are
earned via monthly spending thresholds or manually assigned by staff.

**Decisions:**

- **Who assigns tiers:** Both MANAGER and ADMIN can manually assign a tier to a user.
- **Tier ladder:** Defined in the DB (not hardcoded config). ADMIN-only creates/manages tiers.
  Tiers are ordered by an `index` (or `rank`) field so the system knows which is the entry
  tier and what comes after.
- **Manual assignment:** Staff can assign any tier — no "one step at a time" restriction.
- **Auto tier changes (monthly):** At the end of each month, the system evaluates each user's
  spending and promotes or demotes accordingly. However, a user **never loses the entry-level
  (minimum index) tier once earned.** Example: Gold is index 0 (minimum). A user earns Gold,
  then gets promoted to Platinum. If spending drops the next month, they fall back to Gold —
  never below it.
- **UI:** Admin panel endpoint (REST) for now. Manager/Admin can look up a user and assign a tier.
- **No-tier state:** Loyalty profile exists from day one with `tier = null`. Frontend shows an
  incentive message indicating how much the user needs to spend to enter the tier system.

- **Spending window:** Calendar month (1st–last day of month).
- **Evaluation trigger:** Nightly scheduled job. Runs every night, but tier changes only apply
  when crossing a month boundary (i.e. evaluates the completed month's spending).
- **Permanent override:** The loyalty profile has a `permanent` flag (ADMIN-only toggle).
  - `permanent = false` (default): monthly job evaluates the user normally and may promote/demote.
  - `permanent = true`: monthly job skips this user entirely — their tier is frozen as-is.
  - Managers can assign any tier, but cannot set the `permanent` flag. Only ADMIN can lock it.

- **Spending calculation:**
  - Counted from the **payment date** (not delivery date). An order counts toward the month it was paid.
  - Only **paid orders** are included — unpaid/cancelled orders are ignored.
  - **Refunded orders are subtracted** from the monthly total.

**Status: Implemented ✓** (2026-03-21)

**What was built:**
- `LoyaltyProfile` domain record extended with `permanent` and `lowestTierEver` fields.
- DB schema (`V1`): `loyalty_permanent` and `lowest_tier_ever_id` columns added to `users`.
- `PATCH /api/v1/users/{id}/tier` — MANAGER + ADMIN can assign any tier manually.
- `PATCH /api/v1/users/{id}/loyalty-permanent` — ADMIN-only permanent flag toggle.
- `LoyaltyCalculator.evaluateMonthlyTier()` — pure spending-based evaluation with floor enforcement.
- `MonthlyTierEvaluationService` + `MonthlyTierEvaluationJob` (`@Scheduled cron = "0 0 2 1 * *" UTC`).
- `LoadMonthlySpendingPort` / `MonthlySpendingAdapter` — queries net spending (COMPLETED − REFUNDED) by `completed_at`.
- 31 tests passing.

---

## 3. Discount Redesign — Per-SKU, Hierarchy & Conflict Resolution

**Status: Implemented ✓** (2026-03-21)

**Current behavior:**
- Discounts are stored in a `discounts` table, linked by item code (sku code).
- `DiscountEnrichmentAdapter` picks the best (highest `discountValue`) active discount per SKU.
- No formal hierarchy — just "biggest discount wins".
- Loyalty and sale discounts are computed independently; there is no rule about which takes priority.

**Desired changes:**

### 3a. Discount ↔ SKU relationship
- **Already correct ✓.** The schema has a `discount_items(discount_id, item_code)` join table.
  One discount already applies to many SKUs. No data model change needed.

### 3b. Discount hierarchy / priority
- When 2+ discounts are active on the same SKU, which wins?
- Proposal options (to discuss):
  - **Highest discount wins** (current implicit behavior)
  - **Explicit priority field** — each discount has a `priority` integer; highest priority wins
  - **Type-based rules** — e.g. "FLASH_SALE always beats SEASONAL"
- **Decision ✓:** Type-based rules. Each discount type has a fixed, hard-coded rank (e.g.
  `FLASH_SALE > SEASONAL > CLEARANCE`). When multiple discounts are active on the same SKU,
  the one with the highest-ranked type wins — regardless of its numeric discount value.
  Rationale: gives the business explicit, predictable control over which campaigns dominate,
  without relying on whoever happened to enter a bigger number.

### 3c. Loyalty vs. Sale discount — stacking rules
- Current: sale discount and loyalty discount are applied sequentially (loyalty on top of sale).
  Example: 250 → -15% sale → 212.50 → -15% loyalty → 180.63.
- Possible alternatives:
  - **Stack** (current): loyalty applies on top of sale price (both apply, multiply)
  - **Best-of**: apply whichever gives the lower final price, not both
  - **Sale only**: when a sale is active, loyalty is suppressed for that SKU
  - **Loyalty only**: when user has loyalty, sale discount is replaced by loyalty discount
- **Decision ✓:** Best-of. The system computes both prices independently
  (`originalPrice × (1 − saleDiscount)` and `originalPrice × (1 − loyaltyDiscount)`)
  and applies whichever yields the lower final price. They never stack.
  Rationale: simpler for the customer to understand ("you always get the better deal"),
  and prevents runaway double-discounting.

### 3d. Discount types — definition & management
- **Decision ✓:** Types are stored in a DB table (not a Java enum) so ADMIN can add/rearrange them.
  Initial 4 types (rank order, 1 = highest priority):
  ```
  1. FLASH_SALE
  2. CLEARANCE
  3. SEASONAL
  4. PROMOTIONAL
  ```
- A new `discount_types` table: `id`, `name`, `rank` (integer, lower = higher priority), `created_at`.
- `discounts` table gets a `discount_type_id` FK.
- ADMIN-only REST endpoints to create, reorder, and delete types.
- Conflict resolution (3b) uses `rank` from this table: lowest `rank` integer wins.

**All decisions resolved:**
- [x] Discount ↔ SKU data model → Already many-to-many, no change needed
- [x] Conflict resolution → Type-based rules (DB-managed rank)
- [x] Loyalty vs. sale stacking → Best-of
- [x] Same-type tie-break → Data error. Application layer rejects creating/activating a discount
  whose type already has an active discount on any of the target SKUs for the same date range.
  Enforcement at the application layer (pre-save validation).
- [x] `external_rule_id` → Drop it. Sync infrastructure is gone (Phase 10). New Flyway migration
  drops the column; all ports/methods referencing it are replaced with id-based equivalents.
- [x] Discount value type → Percentage only for now. Fixed-amount adds complexity (currency
  handling, best-of comparison with loyalty). Add later if needed.
- [x] Discount types → Stored in `discount_types` DB table (ADMIN-managed). Initial 4 types
  in rank order: `FLASH_SALE(1) > CLEARANCE(2) > SEASONAL(3) > PROMOTIONAL(4)`.
  ADMIN can add/reorder via REST.
- [x] Discount deletion → **Disable only** (soft). No hard delete. `is_disabled` toggle is the
  only removal mechanism. Preserves audit trail for historical orders.
- [x] Discount type deletion → **Block** if any discounts reference the type. ADMIN must
  manually change those discounts to a different type first, then delete. Error response
  includes the count of discounts blocking deletion. Rationale: Reassign-on-delete risks
  creating same-type conflicts on overlapping SKUs; Block is safe and explicit.
- [x] Who manages discounts → MANAGER + ADMIN can create, edit, and disable discounts.
  Discount type management (create/reorder/delete types) is ADMIN-only.
- [x] ADMIN discount list → Paginated list with filters: by type, by active status
  (active now / scheduled / expired / disabled), by date range. `LoadDiscountPort` needs
  a filtered `findAll(DiscountFilter, Pageable)` query. Each row shows: title, type,
  value %, valid_from/upto, is_disabled, SKU count.

---

## Implementation Plan

### Phase 1 — Pricing Display (Section 1)

**Status:** In progress

Files modified in dependency order:

| # | File | Change |
|---|---|---|
| 1 | `application/readmodel/SkuDto.java` | Remove `price`; add `originalPrice`, `discountPrice`, `discountPercentage`, `discountName`, `discountColorHex`, `loyaltyPrice` |
| 2 | `application/readmodel/ListingVariantDto.java` | Remove `minPrice`, `maxPrice`, `tierDiscountedMinPrice`; add 7 new fields; rewrite `withLoyaltyPrice` → `withLoyalty` |
| 3 | `application/readmodel/ListingVariantDetailDto.java` | Same as #2; `withLoyalty` also cascades to each SKU in the list |
| 4 | `infrastructure/persistence/adapter/DiscountEnrichmentAdapter.java` | Add `isPartialDiscount` to `DiscountInfo`; compute it in `resolveForVariants` |
| 5 | `infrastructure/persistence/adapter/ListingGridRowMapper.java` | Update `toGridDto` and `loadSkuMap` to new DTO signatures |
| 6 | `infrastructure/persistence/adapter/ListingReadAdapter.java` | Rewrite `toSkuDto` and `toDetailDto`; winning-SKU selection replaces `minPrice`/`maxPrice` |
| 7 | `infrastructure/search/ListingSearchAdapter.java` | Update `toDto`, enrichment loop, and `loadSkuMap` |
| 8 | `infrastructure/web/TierPricingEnricher.java` | Call `withLoyalty` instead of `withLoyaltyPrice`; cascade loyalty to per-SKU list |
| 9 | `test/.../ListingControllerTest.java` | Update fake DTO constructors to new signatures; add pricing assertions |

**Key derivation rules:**

Grid-path variant fields (from `DiscountInfo`):
- `originalPrice` = `discountInfo.originalPrice()` if discount exists, else `minOriginalPrice` from query
- `discountPrice` = `discountInfo.discountedPrice()` if discount, else `null`
- `discountPercentage` = `discountInfo.discountPercentage().intValue()` if discount, else `null`
- `discountName` = `discountInfo.saleTitle()` if discount, else `null`
- `discountColorHex` = `discountInfo.saleColorHex()` if discount, else `null`
- `isPartialDiscount` = `discountInfo.isPartialDiscount()` if discount, else `false`
- `loyaltyPrice`, `loyaltyPercentage` = `null` at construction; stamped by `TierPricingEnricher`

Detail-path SKU fields (from `SkuEntity` + `DiscountEntity`):
- `originalPrice` = `SkuEntity.getOriginalPrice()`
- `discountPrice` = `originalPrice * (1 - discount.discountValue / 100)` if discount, else `null`
- `loyaltyPrice` = `null` at construction; stamped by `TierPricingEnricher` via cascade in `withLoyalty`

`TierPricingEnricher.withLoyalty` formula:
```
effectivePct = max(discountPercentage ?? 0, loyaltyTierPct)
loyaltyPrice  = originalPrice * (1 - effectivePct / 100)
loyaltyPercentage = loyaltyTierPct   ← user's own tier %, not effectivePct
```
Applied at variant level AND per-SKU level (detail page).
