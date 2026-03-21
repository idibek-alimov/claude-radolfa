# Radolfa — Planned Changes Discussion

> This file is a working document. We list what needs to change, discuss decisions,
> then convert each item into a concrete implementation plan/phases.

---

## 1. Pricing Display — `ListingVariantDto` & `ListingVariantDetailDto`

**Status:** Needs discussion before touching code.

**Goal:** Product cards and detail pages should show rich pricing info:
- Original price (strikethrough)
- Sale discounted price + discount % badge
- Discount name + color badge (e.g. "Winter Collection" in blue)
- Loyalty/tier price as "Your price" (for authenticated users with a tier)
- Loyalty % badge (visible to guests too, as a marketing signal)

**Known issues with current DTO:**
- `maxPrice` is semantically wrong — it is `MAX(originalPrice)` across ALL SKUs,
  but for the strikethrough we need the original price of the same SKU that produced `minPrice`.
  Proposal: rename to `originalMinPrice`.
- `discountPercentage`, `discountName`, `discountColorHex` exist inside
  `DiscountEnrichmentAdapter.DiscountInfo` but are never wired into the DTO.
- `loyaltyPercentage` is missing from the DTO entirely.

**Open decisions:**
- [ ] Confirm rename: `maxPrice` → `originalMinPrice`
- [ ] Confirm fields to add: `discountPercentage`, `discountName`, `discountColorHex`, `loyaltyPercentage`
- [ ] How does `ListingVariantDetailDto` change? (per-SKU vs. variant-level pricing)
- [ ] What does a guest see vs. a logged-in user without a tier vs. a user with a tier?

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

> To be filled in once all decisions above are made.

| Phase | Scope | Status |
|-------|-------|--------|
| TBD | | |
