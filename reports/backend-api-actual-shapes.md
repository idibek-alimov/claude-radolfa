# Backend API — Actual Response Shapes

**Date:** 2026-03-20
**Status:** Migration complete. Backend now returns canonical field names. Frontend adapter layer removed.

---

## Summary

The STANDALONE_MIGRATION is complete. The backend API response shapes now match the
field names defined in `FRONTEND_INTEGRATION_PLAN.md` and `frontend/CLAUDE.md`.
The adapter shim that previously existed in `entities/product/api/index.ts` has been deleted.

---

## 1. `GET /api/v1/listings?page=N&size=M`

```json
{
  "content": [ ... ],
  "totalElements": 90,
  "totalPages": 8,
  "number": 1,
  "size": 12,
  "first": true,
  "last": false
}
```

Pagination is **1-based**. `page=1` returns the first page.

---

## 2. Listing item (grid and detail)

### Grid item
```json
{
  "variantId": 1,
  "slug": "...",
  "colorDisplayName": "Essential Cotton T-Shirt",
  "categoryName": "Tops",
  "colorKey": "midnight-black",
  "colorHex": "#1A1A1A",
  "webDescription": "...",
  "images": [...],
  "minPrice": 25.0,
  "maxPrice": 29.0,
  "tierDiscountedMinPrice": null,
  "topSelling": true,
  "featured": false,
  "productCode": "RD-10001",
  "skus": [
    { "skuId": 10, "skuCode": "RD-10001-S", "sizeLabel": "S", "stockQuantity": 5, "price": 25.0 },
    ...
  ]
}
```

### Price model
| Field | Meaning |
|---|---|
| `minPrice` | Effective lowest SKU price (sale discount already applied if active) |
| `maxPrice` | Raw highest SKU price (for strikethrough when sale is active) |
| `tierDiscountedMinPrice` | Tier-based loyalty price (null for guests / no tier) |

---

## 3. `GET /api/v1/home/collections`

```json
[
  { "key": "featured", "title": "Featured", "listings": [...] },
  { "key": "top-selling", "title": "Top Selling", "listings": [...] }
]
```

---

## 4. `GET /api/v1/home/collections/{key}`

```json
{ "key": "featured", "title": "Featured", "listings": [...] }
```

---

## 5. `GET /api/v1/categories`

```json
[{ "id": 1, "name": "Tops", "slug": "tops", "parentId": null, "children": [] }]
```

---

## Key field renames applied

| Entity | Before | After |
|---|---|---|
| Pagination | `items`, `hasMore`, `page` | `content`, `last`, `number` |
| Pagination (new) | — | `totalPages`, `size`, `first` |
| `ListingVariant` | `id` | `variantId` |
| `ListingVariant` | `name` | `colorDisplayName` |
| `ListingVariant` | `category` | `categoryName` |
| `ListingVariant` | `colorHexCode` | `colorHex` |
| `ListingVariant` | `originalPrice` + `discountedPrice` | `minPrice` + `maxPrice` |
| `ListingVariant` | `loyaltyPrice` | `tierDiscountedMinPrice` |
| `ListingVariant` | `totalStock` (removed) | computed from `skus[].stockQuantity` |
| `Sku` | `id` | `skuId` |
| `Sku` | `originalPrice` | `price` (effective, sale-adjusted) |
| `HomeSection` | `items` | `listings` |
| Removed fields | `discountedPrice`, `saleTitle`, `saleColorHex`, `discountPercentage`, `loyaltyDiscountPercentage` | — |
