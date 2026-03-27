# Frontend Guide: New Product Pricing ReadModels

> The backend has recently moved product DTOs from `tj.radolfa.infrastructure.web.dto` to `tj.radolfa.application.readmodel`. This document outlines the endpoints serving these models and the fields available to the frontend.

---

## 1. Overview of DTO Changes

The frontend will consume two primary DTOs for products. Both retain the dual-discount pricing system (ERP sale discount + Runtime Loyalty discount) structured into distinct fields.

- `ListingVariantDto` — Used for **product cards/grids**. Prices here represent the "starting at" or aggregated minimal prices across all available sizes (SKUs).
- `ListingVariantDetailDto` — Used for the **product detail page (PDP)**. It includes the same base fields as the grid card, but also contains the full list of purchasable `skus` (sizes) and a lightweight `siblingVariants` array for rendering colour swatches without another API call.

---

## 2. Endpoints Serving These DTOs

### Endpoints returning `ListingVariantDto` (Card / Grid)
These endpoints serve arrays or paginated results of the compact `ListingVariantDto`.

| Endpoint | Method | Format | Use Case |
|----------|--------|--------|----------|
| `/api/v1/listings` | `GET` | `PageResult<ListingVariantDto>` | Main storefront catalog. Query with `page` and `limit`. |
| `/api/v1/listings/search` | `GET` | `PageResult<ListingVariantDto>` | Search results. Query with `q`, `page`, `limit`. |
| `/api/v1/home/collections` | `GET` | `List<HomeSectionDto>` | Homepage rows (Featured, New Arrivals). The `items` array is `ListingVariantDto[]`. |
| `/api/v1/home/collections/{key}` | `GET` | `CollectionPageDto` | Paginated view of a single homepage collection. Contains `PageResult<ListingVariantDto>`. |

### Endpoints returning `ListingVariantDetailDto` (Detail)
| Endpoint | Method | Format | Use Case |
|----------|--------|--------|----------|
| `/api/v1/listings/{slug}` | `GET` | `ListingVariantDetailDto` | Single product detail page. |

> **Authentication**: All endpoints above are public (`GET`). However, if a JWT is present in the request headers/cookies, the backend will enrich the response with the `loyaltyPrice` and `loyaltyDiscountPercentage` based on the user's tier.

---

## 3. Schema & Fields Reference

### `ListingVariantDto` Schema

```typescript
interface ListingVariantDto {
  id: number;
  slug: string;
  name: string;
  category: string | null;
  colorKey: string;
  colorHexCode: string | null;
  webDescription: string | null;
  images: string[];
  
  // -- PRICING FIELDS (Aggregated across SKUs) --
  originalPrice: number;              // Always present (The base ERP price)
  discountedPrice: number | null;     // Non-null if an active ERP 'sale' is running
  loyaltyPrice: number | null;        // Non-null if user is logged in & has loyalty discount
  discountPercentage: number | null;  // E.g., 15.00 for a 15% ERP sale
  loyaltyDiscountPercentage: number | null; // E.g., 5.00 for a GOLD tier user
  
  // -- INVENTORY & STATUS --
  totalStock: number;                 // Sum of all sizes' stock
  topSelling: boolean;
  featured: boolean;
}
```

### `ListingVariantDetailDto` Schema

Inherits all fields of the grid card, plus relations for the Detail page:

```typescript
interface ListingVariantDetailDto {
  // ... (All fields from ListingVariantDto above) ...
  
  skus: SkuDto[]; // Individual sizes with their specific pricing, stock, & expiry
  siblingVariants: SiblingVariant[]; // Colour swatches
}
```

#### SiblingVariant Schema
Used to build colour swatches on the detail page:
```typescript
interface SiblingVariant {
  slug: string;
  colorKey: string;
  colorHexCode: string | null;
  thumbnail: string | null; // The first image of the sibling, for swatch tooltip/preview
}
```

#### SkuDto Schema
Used to build the size selector and price the item accurately once a size is picked:
```typescript
interface SkuDto {
  id: number;
  erpItemCode: string;
  sizeLabel: string;
  stockQuantity: number;
  
  // -- SKU SPECIFIC PRICING --
  originalPrice: number;
  discountedPrice: number | null;
  loyaltyPrice: number | null;
  discountPercentage: number | null;
  loyaltyDiscountPercentage: number | null;
  
  onSale: boolean;
  discountedEndsAt: string | null; // ISO-8601 instant, null if onSale = false
}
```

---

## 4. Frontend Recommended Logic

The logic remains identical to the previous implementation, just using the new DTO imports. 

When displaying prices (both on cards and after a user selects a SKU):
1. **Current Price**: Decide the final price using `loyaltyPrice ?? discountedPrice ?? originalPrice`.
2. **Strikethroughs**: 
   - If `loyaltyPrice != null`, strike through `originalPrice` and (if present) `discountedPrice`.
   - If `discountedPrice != null` but no loyalty price, strike through `originalPrice`.
3. **Badges**: 
   - Show `-{discountPercentage}%` if `discountPercentage != null`.
   - Show `Extra -{loyaltyDiscountPercentage}% Loyalty` if `loyaltyDiscountPercentage != null`.

The backend handles all math, rounding, stacking of discounts, and expiry filtering. The frontend purely renders the fields returned.
