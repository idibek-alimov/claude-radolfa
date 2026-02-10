# Role and Objective
You are a Senior Frontend Engineer (React/Next.js/TypeScript).
Your task is to **migrate the frontend to the new 3-Tier Product Hierarchy API**.

**Current State:**
The frontend uses a "Flat Product" model (`Product` interface) and calls `/api/v1/products`.

**Target State:**
The frontend must use the new `ListingVariant` model and call `/api/v1/listings`.
*   **Grid**: Display "Product Cards" (ListingVariants).
*   **Detail**: Display "Product Detail" (ListingVariantDetail) with Size Selector (SKUs) and Color Swatches (SiblingVariants).

---

# 🛑 INTERACTIVE PROCESS (MANDATORY)

1.  **READ & RATE**: Read this prompt. Rate the migration plan.
2.  **PROPOSE PLAN**: List files to modify/create.
3.  **WAIT**: Ask for user confirmation.
4.  **EXECUTE**: Code only after approval.

---

# 1. Type Definitions Update
**File:** `src/entities/product/model/types.ts`

**DELETE** the old `Product` interface.
**CREATE** these new interfaces (matching backend DTOs):

```typescript
export interface Sku {
  id: number;
  erpItemCode: string;
  sizeLabel: string;
  stockQuantity: number;
  price: number;
  salePrice: number;
  onSale: boolean;
  saleEndsAt: string | null;
}

export interface ListingVariant {
  id: number;
  slug: string;
  name: string;
  colorKey: string;
  webDescription: string;
  images: string[];
  priceStart: number;
  priceEnd: number;
  totalStock: number;
}

export interface SiblingVariant {
  slug: string;
  colorKey: string;
  thumbnail: string;
}

export interface ListingVariantDetail extends ListingVariant {
  skus: Sku[];
  siblingVariants: SiblingVariant[];
}
```

---

# 2. API Client Update
**File:** `src/entities/product/api/index.ts` (or `productApi.ts`)

*   **Remove:** `getProducts`, `getProduct`, `getTopSelling`. (The old flat endpoints).
*   **Add:**
    *   `getListings(page, limit)` -> calls `GET /api/v1/listings`
    *   `getListingBySlug(slug)` -> calls `GET /api/v1/listings/{slug}`
    *   `searchListings(query)` -> calls `GET /api/v1/listings/search`
    *   `getAutocomplete(query)` -> calls `GET /api/v1/listings/autocomplete`

---

# 3. UI Component Refactoring

### A. Product Card (`src/entities/product/ui/ProductCard.tsx`)
*   Update props to accept `ListingVariant`.
*   Display `priceStart` instead of just `price`.
*   Link to `/products/{slug}`.

### B. Product Grid (`src/widgets/ProductGrid.tsx` or similar)
*   Update data fetching to use `getListings`.
*   Pass `ListingVariant` to `ProductCard`.

### C. Product Detail Page (`src/app/(storefront)/products/[id]/page.tsx`)
*   **Refactor**: Rename folder `[id]` to `[slug]`.
*   **Logic**:
    1.  Fetch data using `getListingBySlug(params.slug)`.
    2.  State: Add `selectedSku` state.
    3.  **Render**:
        *   **Images**: Show images from `ListingVariantDetail`.
        *   **Description**: Show `webDescription`.
        *   **Color Swatches**: Loop through `siblingVariants`. Render links to their slugs.
        *   **Size Selector**: Loop through `skus`.
            *   Show `sizeLabel`.
            *   Disable if `stockQuantity === 0`.
            *   On click -> `setSelectedSku(sku)`.
        *   **Price**: Show `selectedSku.salePrice` (or range if no size selected).
        *   **Add to Cart**: Only enabled if `selectedSku` is set.

---

# 4. Search Bar Update
**File:** `src/features/search/...`
*   Update to use `searchListings` and `getAutocomplete`.
*   Navigate to the new slug-based URLs.

---

# 5. Constraints
*   **Strict Types**: No `any`. Use the interfaces defined above.
*   **Clean Up**: Identify and remove dead code related to the old "Flat Product" model (unless it's used in the Admin/Manager panel - CHECK FIRST).
    *   *Note*: The Admin panel likely still uses `ProductController` (Legacy). If so, **keep the old `Product` type** but maybe rename it to `AdminProduct` or keep it separate to avoid confusion.
