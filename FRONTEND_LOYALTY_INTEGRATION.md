# Loyalty Tiers — Frontend Integration Guide

> **Date:** 2026-03-14
> **Status:** Backend complete, ready for frontend integration.
> **Breaking Change:** `loyaltyPoints` field on the user object has been replaced with a nested `loyalty` object.

---

## 1. What Changed

The backend now supports multi-tier loyalty programs (Gold, Platinum, Titanium, etc.) synced from ERPNext. The key changes that affect the frontend are:

1. **`GET /api/v1/auth/me`** — The `UserDto` response shape changed. The flat `loyaltyPoints: number` field is replaced with a nested `loyalty` object containing points, tier info, and spending progress.
2. **`GET /api/v1/loyalty-tiers`** — New endpoint returning all available tiers ordered by `displayOrder`.
3. **Tier-aware pricing** — Product endpoints now return both **base prices** and **tier-discounted prices** (`tierPriceStart`, `tierPriceEnd`, `tierPrice`) computed server-side via `TierPricingEnricher`. The frontend should display the tier prices when available, falling back to base prices for unauthenticated users.

---

## 2. API Endpoints

### 2.1 `GET /api/v1/auth/me` (Protected)

Returns the currently authenticated user. **Auth required** (cookie-based JWT).

#### Response — BEFORE (old shape)
```json
{
  "id": 123,
  "phone": "+992918123456",
  "role": "USER",
  "name": "John Doe",
  "email": "john@example.com",
  "loyaltyPoints": 200,
  "enabled": true
}
```

#### Response — AFTER (new shape)
```json
{
  "id": 123,
  "phone": "+992918123456",
  "role": "USER",
  "name": "John Doe",
  "email": "john@example.com",
  "loyalty": {
    "points": 200,
    "tier": {
      "name": "PLATINUM",
      "discountPercentage": 15.00,
      "cashbackPercentage": 7.50,
      "minSpendRequirement": 50000.00,
      "displayOrder": 2
    },
    "spendToNextTier": 25000.00,
    "spendToMaintainTier": 0.00,
    "currentMonthSpending": 15000.00
  },
  "enabled": true
}
```

#### Field Descriptions

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `loyalty.points` | `number` | No | Raw loyalty points accumulated by the user |
| `loyalty.tier` | `object` | **Yes** | Current tier. `null` if user has no tier assigned yet |
| `loyalty.tier.name` | `string` | No | Tier name (e.g., "GOLD", "PLATINUM") |
| `loyalty.tier.discountPercentage` | `number` | No | Discount % to apply on catalog base prices |
| `loyalty.tier.cashbackPercentage` | `number` | No | Cashback % earned on purchases |
| `loyalty.tier.minSpendRequirement` | `number` | No | Minimum monthly spend to qualify for this tier |
| `loyalty.tier.displayOrder` | `number` | No | Display order. Lower = higher tier (1 is best) |
| `loyalty.spendToNextTier` | `number` | **Yes** | Amount remaining to reach the next tier. `null` if already at highest tier. For users with **no tier**, the backend auto-computes this as the gap to the entry-level tier. |
| `loyalty.spendToMaintainTier` | `number` | **Yes** | Amount remaining this month to keep current tier. `null` if no tier |
| `loyalty.currentMonthSpending` | `number` | **Yes** | Total spent in the current calendar month |

> **Note on JSON number types:** The backend guarantees `BigDecimal` values serialize as plain JSON numbers (e.g., `15.00`, not `"15.00"` or `1.5E+1`). This is enforced via Jackson's `write-bigdecimal-as-plain: true` configuration.

---

### 2.2 `GET /api/v1/loyalty-tiers` (Protected)

Returns **all** available loyalty tiers, ordered by `displayOrder` ascending (best tier first).

**Auth:** Required (any authenticated user).

#### Response
```json
[
  {
    "name": "TITANIUM",
    "discountPercentage": 20.00,
    "cashbackPercentage": 10.00,
    "minSpendRequirement": 100000.00,
    "displayOrder": 1
  },
  {
    "name": "PLATINUM",
    "discountPercentage": 15.00,
    "cashbackPercentage": 7.50,
    "minSpendRequirement": 50000.00,
    "displayOrder": 2
  },
  {
    "name": "GOLD",
    "discountPercentage": 5.00,
    "cashbackPercentage": 2.50,
    "minSpendRequirement": 10000.00,
    "displayOrder": 3
  }
]
```

Use this endpoint to render:
- A full tier ladder / progress visualization
- "Next tier" comparisons
- Tier benefit breakdowns

---

### 2.3 Product Endpoints — Tier Pricing Fields (Automatic)

Product listing and detail endpoints now include **server-computed tier prices** for authenticated users. No additional API calls needed — the backend resolves the user's discount from the JWT.

#### Listing Cards (`GET /listings`, `/categories/*/listings`, `/home/collections`)

```json
{
  "id": 42,
  "slug": "cotton-tshirt-red",
  "priceStart": 100.00,
  "priceEnd": 150.00,
  "tierPriceStart": 85.00,
  "tierPriceEnd": 127.50,
  "..."
}
```

#### Product Detail (`GET /listings/{slug}`)

```json
{
  "id": 42,
  "tierPriceStart": 85.00,
  "tierPriceEnd": 127.50,
  "skus": [
    {
      "id": 101,
      "sizeLabel": "M",
      "price": 100.00,
      "salePrice": null,
      "tierPrice": 85.00,
      "..."
    }
  ]
}
```

| Field | Type | Nullable | When `null` |
|-------|------|----------|-------------|
| `tierPriceStart` | `number` | **Yes** | User not authenticated or has no tier |
| `tierPriceEnd` | `number` | **Yes** | User not authenticated or has no tier |
| `tierPrice` (SKU) | `number` | **Yes** | User not authenticated or has no tier |

---

## 3. Breaking Changes — Migration Guide

### 3.1 Update the `User` type

**File:** `frontend/src/entities/user/model/types.ts`

Replace:
```typescript
export interface User {
  id: number | null;
  phone: string;
  role: UserRole;
  name?: string;
  email?: string;
  loyaltyPoints: number;  // REMOVE THIS
  enabled: boolean;
}
```

With:
```typescript
export interface LoyaltyTier {
  name: string;
  discountPercentage: number;
  cashbackPercentage: number;
  minSpendRequirement: number;
  displayOrder: number;
}

export interface LoyaltyProfile {
  points: number;
  tier: LoyaltyTier | null;
  spendToNextTier: number | null;
  spendToMaintainTier: number | null;
  currentMonthSpending: number | null;
}

export interface User {
  id: number | null;
  phone: string;
  role: UserRole;
  name?: string;
  email?: string;
  loyalty: LoyaltyProfile;  // NEW nested object
  enabled: boolean;
}
```

### 3.2 Fix all references to `loyaltyPoints`

Search the frontend codebase for `loyaltyPoints` and replace with `loyalty.points`. Common locations:
- Auth hooks / user state
- Profile pages
- Any component displaying points

### 3.3 Update `POST /api/v1/auth/verify` response handling

The verify endpoint returns `AuthResponseDto` which contains a `user` field. That user object now uses the new shape. Ensure the auth store / context parses the nested `loyalty` object correctly.

---

## 4. Frontend Responsibilities

### 4.1 Tier Pricing (Server-Side Enriched)

The backend **computes tier-discounted prices server-side** via `TierPricingEnricher`. Product listing and detail endpoints now include additional fields:

| Endpoint | New Fields |
|----------|------------|
| `GET /listings`, `/categories/*/listings`, `/home/collections` | `tierPriceStart`, `tierPriceEnd` on each listing card |
| `GET /listings/{slug}` | `tierPriceStart`, `tierPriceEnd` on the detail + `tierPrice` on each SKU |

- **Authenticated users with a tier:** `tierPriceStart`/`tierPriceEnd`/`tierPrice` will contain the discounted values.
- **Unauthenticated users or users without a tier:** These fields will be `null`. Use `priceStart`/`priceEnd`/`price` as fallback.

```typescript
function getDisplayPrice(listing: ListingVariant): number {
  return listing.tierPriceStart ?? listing.priceStart;
}
```

Display both prices to the user when tier pricing is available:
- ~~Base price~~ (strikethrough)
- **Tier price** (highlighted)

> **Note:** The `TierPricingEnricher` resolves the user's discount from the JWT security context, so no extra API calls are needed — product endpoints automatically return tier-aware prices for logged-in users.

### 4.2 Tier Progress UI

Using data from both `/auth/me` and `/loyalty-tiers`:

```
Current: GOLD (5% discount)
├── Spent this month: $15,000
├── To maintain GOLD: $0 (already met)
├── To reach PLATINUM: $25,000 more
│
│   ████████████░░░░░░░░  60% to PLATINUM
│
└── Points: 200
```

### 4.3 Null Tier Handling

When `loyalty.tier === null`:
- User has no tier (new user or below minimum spend)
- Show a "Join our loyalty program" or "Start earning" CTA
- Do NOT apply any discount to prices (tier price fields will be `null` in product responses)
- `spendToNextTier` is **auto-populated by the backend** with the gap to the entry-level tier, so you can show progress directly without cross-referencing `/loyalty-tiers`

---

## 5. Suggested File Structure (FSD)

```
frontend/src/
├── entities/
│   └── loyalty/
│       ├── model/
│       │   └── types.ts          # LoyaltyTier, LoyaltyProfile interfaces
│       └── api.ts                # getLoyaltyTiers() query hook (data fetching)
│
├── features/
│   └── loyalty/
│       └── ui/
│           ├── LoyaltyCard.tsx   # User's current loyalty status
│           ├── TierProgress.tsx  # Progress bar to next tier
│           └── TiersList.tsx     # All available tiers comparison
│
├── widgets/
│   └── loyalty-dashboard/
│       └── LoyaltyDashboard.tsx  # Composed widget for profile page
```

> **FSD Note:** The query hook for fetching tier data belongs in `entities/loyalty/api.ts` (pure data fetching), not in `features/` (reserved for user actions/mutations).

---

## 6. API Call Example (TanStack Query)

```typescript
// entities/loyalty/api.ts
import apiClient from "@/shared/api/axios";
import type { LoyaltyTier } from "@/entities/loyalty/model/types";

export function useLoyaltyTiers() {
  return useQuery({
    queryKey: ["loyalty-tiers"],
    queryFn: () =>
      apiClient.get<LoyaltyTier[]>("/api/v1/loyalty-tiers").then(r => r.data),
    staleTime: 5 * 60 * 1000, // tiers rarely change, cache 5 min
  });
}
```

---

## 7. Data Flow Summary

```
ERPNext (Source of Truth)
    │
    ├── POST /sync/loyalty-tiers    → Backend stores tier definitions
    ├── POST /sync/loyalty          → Backend updates user's tier + points + spending
    └── POST /sync/users            → Backend updates user's tier + points + spending
                │
                ▼
        Backend Database
                │
                ├── GET /auth/me           → Frontend gets user with loyalty profile
                └── GET /loyalty-tiers     → Frontend gets all tier definitions
                        │
                        ▼
                Frontend (Display Only)
                ├── Show tier badge & benefits
                ├── Show progress to next tier
                ├── Display tier prices from product endpoints (server-computed)
                ├── Fall back to base prices for unauthenticated users
                └── Show points balance
```

**The frontend CANNOT modify** loyalty data. It is strictly read-only, synced from ERPNext via the backend.

---

## 8. Edge Cases to Handle

| Scenario | `loyalty.tier` | `spendToNextTier` | What to show |
|----------|---------------|-------------------|--------------|
| New user, no purchases | `null` | Auto-computed gap to entry tier | "Start earning!" CTA with progress bar |
| User below first tier | `null` | Auto-computed gap to entry tier | Progress bar to first tier (no cross-referencing needed) |
| User at GOLD tier | `{name: "GOLD", ...}` | `25000.00` | Progress bar to PLATINUM |
| User at highest tier | `{name: "TITANIUM", ...}` | `null` | "You're at the top!" badge |
| User at risk of demotion | `{...}` | — | `spendToMaintainTier > 0` → show warning |

---

## 9. Sync Endpoints (Admin Only — For Reference)

These endpoints are called by the ERPNext integration system (SYSTEM role only). Frontend does NOT call these, but they're documented here for completeness:

| Endpoint | Method | Purpose | Idempotency |
|----------|--------|---------|-------------|
| `/api/v1/sync/loyalty-tiers` | POST | Push tier definitions from ERPNext | Required |
| `/api/v1/sync/loyalty` | POST | Push user loyalty data (points + tier + spending) | Required |
| `/api/v1/sync/users` | POST | Push user profile (includes tier + spending) | Not required |
| `/api/v1/sync/users/batch` | POST | Batch user sync | Not required |
