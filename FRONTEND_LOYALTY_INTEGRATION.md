# Loyalty Tiers — Frontend Integration Guide

> **Date:** 2026-03-14
> **Status:** Backend complete, ready for frontend integration.
> **Breaking Change:** `loyaltyPoints` field on the user object has been replaced with a nested `loyalty` object.

---

## 1. What Changed

The backend now supports multi-tier loyalty programs (Gold, Platinum, Titanium, etc.) synced from ERPNext. The key changes that affect the frontend are:

1. **`GET /api/v1/auth/me`** — The `UserDto` response shape changed. The flat `loyaltyPoints: number` field is replaced with a nested `loyalty` object containing points, tier info, and spending progress.
2. **`GET /api/v1/loyalty-tiers`** — New endpoint returning all available tiers ordered by rank.
3. **Price display shift** — All catalog prices from the backend are **base prices**. The frontend is now responsible for applying the user's `loyalty.tier.discountPercentage` to compute the displayed price.

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
      "rank": 2
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
| `loyalty.tier.rank` | `number` | No | Display order. Lower = higher tier (1 is best) |
| `loyalty.spendToNextTier` | `number` | **Yes** | Amount remaining to reach the next tier. `null` if already at highest tier |
| `loyalty.spendToMaintainTier` | `number` | **Yes** | Amount remaining this month to keep current tier. `null` if no tier |
| `loyalty.currentMonthSpending` | `number` | **Yes** | Total spent in the current calendar month |

> **Note on JSON number types:** Java `BigDecimal` fields may serialize as strings (e.g., `"15.00"`) depending on Jackson config. Use `parseFloat()` or `Number()` when consuming these values.

---

### 2.2 `GET /api/v1/loyalty-tiers` (Public)

Returns **all** available loyalty tiers, ordered by `rank` ascending (best tier first).

**Auth:** Not required.

#### Response
```json
[
  {
    "name": "TITANIUM",
    "discountPercentage": 20.00,
    "cashbackPercentage": 10.00,
    "minSpendRequirement": 100000.00,
    "rank": 1
  },
  {
    "name": "PLATINUM",
    "discountPercentage": 15.00,
    "cashbackPercentage": 7.50,
    "minSpendRequirement": 50000.00,
    "rank": 2
  },
  {
    "name": "GOLD",
    "discountPercentage": 5.00,
    "cashbackPercentage": 2.50,
    "minSpendRequirement": 10000.00,
    "rank": 3
  }
]
```

Use this endpoint to render:
- A full tier ladder / progress visualization
- "Next tier" comparisons
- Tier benefit breakdowns

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
  rank: number;
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

### 4.1 Price Calculation (Critical)

The backend returns **base prices** for all catalog products. The frontend must apply the tier discount:

```typescript
function getDiscountedPrice(basePrice: number, user: User): number {
  if (!user.loyalty.tier) return basePrice;
  const discount = user.loyalty.tier.discountPercentage / 100;
  return basePrice * (1 - discount);
}
```

Display both prices to the user:
- ~~Base price~~ (strikethrough)
- **Discounted price** (highlighted)

Only apply the discount when a user is authenticated AND has a tier assigned.

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
- Do NOT apply any discount to prices
- Show the first tier's `minSpendRequirement` as the goal

---

## 5. Suggested File Structure (FSD)

```
frontend/src/
├── entities/
│   └── loyalty/
│       └── model/
│           └── types.ts          # LoyaltyTier, LoyaltyProfile interfaces
│
├── features/
│   └── loyalty/
│       ├── api.ts                # getLoyaltyTiers() API call
│       └── ui/
│           ├── LoyaltyCard.tsx   # User's current loyalty status
│           ├── TierProgress.tsx  # Progress bar to next tier
│           └── TiersList.tsx     # All available tiers comparison
│
├── widgets/
│   └── loyalty-dashboard/
│       └── LoyaltyDashboard.tsx  # Composed widget for profile page
```

---

## 6. API Call Example (TanStack Query)

```typescript
// features/loyalty/api.ts
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
                ├── Apply discount % to catalog base prices
                └── Show points balance
```

**The frontend CANNOT modify** loyalty data. It is strictly read-only, synced from ERPNext via the backend.

---

## 8. Edge Cases to Handle

| Scenario | `loyalty.tier` | `spendToNextTier` | What to show |
|----------|---------------|-------------------|--------------|
| New user, no purchases | `null` | `null` | "Start earning!" CTA |
| User below first tier | `null` | `null` | Progress to first tier using `minSpendRequirement` from `/loyalty-tiers` |
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
