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

### 3.1 Create `entities/loyalty/model/types.ts`

Define loyalty types in their own FSD entity. The `User` type will import from here.

**New file:** `frontend/src/entities/loyalty/model/types.ts`

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
```

### 3.2 Update the `User` type

**File:** `frontend/src/entities/user/model/types.ts`

```diff
+ import type { LoyaltyProfile } from "@/entities/loyalty/model/types";
+
  export interface User {
    id: number | null;
    phone: string;
    role: UserRole;
    name?: string;
    email?: string;
-   loyaltyPoints: number;
+   loyalty: LoyaltyProfile;
    enabled: boolean;
  }
```

### 3.3 Update the admin `UserDto` type

**File:** `frontend/src/features/user-management/types.ts`

The admin user management panel also has a `UserDto` with `loyaltyPoints`. Update it the same way:

```diff
+ import type { LoyaltyProfile } from "@/entities/loyalty/model/types";
+
  export interface UserDto {
    id: number;
    phone: string;
    role: "USER" | "MANAGER" | "SYSTEM";
    name?: string;
    email?: string;
-   loyaltyPoints: number;
+   loyalty: LoyaltyProfile;
    enabled: boolean;
  }
```

### 3.4 Update product types with tier pricing fields

**File:** `frontend/src/entities/product/model/types.ts`

```diff
  export interface ListingVariant {
    id: number;
    slug: string;
    name: string;
    category: string;
    colorKey: string;
    colorHexCode: string | null;
    webDescription: string;
    images: string[];
    priceStart: number;
    priceEnd: number;
+   tierPriceStart: number | null;
+   tierPriceEnd: number | null;
    totalStock: number;
    topSelling: boolean;
    featured: boolean;
  }

  export interface Sku {
    id: number;
    erpItemCode: string;
    sizeLabel: string;
    stockQuantity: number;
    price: number;
    salePrice: number;
+   tierPrice: number | null;
    onSale: boolean;
    saleEndsAt: string | null;
  }
```

### 3.5 Fix all references to `loyaltyPoints`

Search the frontend codebase for `loyaltyPoints` and replace with `loyalty.points`. Known locations:
- `entities/user/model/types.ts` — type definition (Section 3.2)
- `features/user-management/types.ts` — admin DTO (Section 3.3)
- `features/auth/model/useAuth.ts` — auth hook state
- `app/(storefront)/profile/page.tsx` — profile page display
- Navbar user dropdown — points badge

### 3.6 Update `POST /api/v1/auth/verify` response handling

The verify endpoint returns `AuthResponseDto` which contains a `user` field. That user object now uses the new shape.

**File:** `frontend/src/features/auth/model/useAuth.ts`

The `useAuth()` hook uses a custom state-based approach (not TanStack Query). Ensure the hook correctly parses the nested `loyalty` object when setting user state from both `/auth/me` and `/auth/verify` responses.

### 3.7 Update `useAuth()` to re-fetch loyalty data after purchases

The `useAuth()` hook fetches user data from `GET /api/v1/auth/me` on mount. Loyalty fields (`points`, `currentMonthSpending`, tier status) can change after a purchase is completed. Expose a `refreshUser()` method (or call the existing `updateUser()`) that re-fetches `/auth/me` so the profile/navbar stay current.

```typescript
// Call after order placement succeeds
const { refreshUser } = useAuth();
await placeOrder(orderData);
refreshUser(); // re-fetch /auth/me to update loyalty state
```

> **Note:** Product listing queries should also be invalidated after login/logout since tier prices change based on auth state. Invalidate any cached listing queries when the auth state transitions.

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
// Listing card price range
function getDisplayPriceRange(listing: ListingVariant) {
  return {
    start: listing.tierPriceStart ?? listing.priceStart,
    end: listing.tierPriceEnd ?? listing.priceEnd,
    hasTierDiscount: listing.tierPriceStart != null,
  };
}

// Individual SKU price (product detail page)
function getSkuDisplayPrice(sku: Sku) {
  return {
    price: sku.tierPrice ?? sku.price,
    originalPrice: sku.tierPrice != null ? sku.price : null,
    hasTierDiscount: sku.tierPrice != null,
  };
}
```

Display both prices to the user when tier pricing is available:
- ~~Base price~~ (strikethrough)
- **Tier price** (highlighted)

> **Note:** The `TierPricingEnricher` resolves the user's discount from the JWT security context, so no extra API calls are needed — product endpoints automatically return tier-aware prices for logged-in users.
>
> **Note on auth state transitions:** When a user logs in or out, cached product listing queries should be invalidated, since tier prices differ based on authentication state. Invalidate query keys like `["listings"]`, `["home-collections"]`, etc. on auth change.

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
│       ├── index.ts              # Barrel: re-exports types + api hook
│       ├── model/
│       │   └── types.ts          # LoyaltyTier, LoyaltyProfile interfaces
│       └── api.ts                # useLoyaltyTiers() query hook (data fetching)
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

> **FSD Note:** Types live in `entities/loyalty/model/types.ts` (canonical location). The `User` type in `entities/user/` imports `LoyaltyProfile` from here — not the other way around. The query hook belongs in `entities/loyalty/api.ts` (pure data fetching), not in `features/` (reserved for user actions/mutations).

---

## 6. API Call Example (TanStack Query)

```typescript
// entities/loyalty/api.ts
import { useQuery } from "@tanstack/react-query";
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
| User at risk of demotion | `{...}` | — | `spendToMaintainTier > 0` → show warning (see below) |

#### Demotion Warning UX

When `spendToMaintainTier > 0`, the user risks losing their current tier at end of month. Display severity based on remaining calendar days:

| Days remaining in month | Severity | UX |
|------------------------|----------|-----|
| > 7 days | Info | Subtle note: "Spend X more to keep your GOLD tier this month" |
| 3–7 days | Warning | Yellow banner on profile + loyalty card |
| 1–2 days | Urgent | Red badge on profile + consider a toast/notification |

```typescript
function getDaysRemainingInMonth(): number {
  const now = new Date();
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
  return lastDay - now.getDate();
}
```

---

## 9. Sync Endpoints (Admin Only — For Reference)

These endpoints are called by the ERPNext integration system (SYSTEM role only). Frontend does NOT call these, but they're documented here for completeness:

| Endpoint | Method | Purpose | Idempotency |
|----------|--------|---------|-------------|
| `/api/v1/sync/loyalty-tiers` | POST | Push tier definitions from ERPNext | Required |
| `/api/v1/sync/loyalty` | POST | Push user loyalty data (points + tier + spending) | Required |
| `/api/v1/sync/users` | POST | Push user profile (includes tier + spending) | Not required |
| `/api/v1/sync/users/batch` | POST | Batch user sync | Not required |
