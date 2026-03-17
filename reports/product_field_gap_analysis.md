# Product Field Gap Analysis: Backend vs. UI Mockup

This report identifies the discrepancies between the current backend product data structure and the requirements of the provided product detail page UI mockup.

## 1. Summary of Identified UI Fields
Based on the provided image, the following data points are required to render the full product page:

### A. General Information
- **Product Name**: "Футболка детская черная хлопок"
- **Brand Name**: "Baby Style"
- **Article Number (Артикул)**: 79653867
- **Breadcrumbs**: Home / Kids / Boys / T-shirts / Baby Style

### B. Social Proof & Trust
- **Rating**: 4.9 stars
- **Total Reviews**: 2,525
- **Questions Count**: 182
- **Verification Badge**: "Оригинал" (Original)
- **Seller Name**: "Вертекс - детская одежда из хлопка"
- **Seller Rating**: 4.9 stars

### C. Pricing & Discounts
- **Current Display Price**: 295 P
- **Previous Price (before current discount)**: 302 P
- **Reference/List Price (Strikethrough)**: 4,444 P
- **Promotion Title**: "СКИДКИ РАСЦВЕЛИ" (Badged on the image)

### D. Physical Attributes & Variants
- **Color Selection**: Visual swatches for other colors (black, white, yellow, etc.)
- **Selected Color Name**: "черный" (black)
- **Size Selection**: Grid of sizes (92, 98, 104, 110, etc.)
- **Composition (Состав)**: "хлопок; Кулирная гладь; хлопковое волокно"
- **Gender (Пол)**: "Мальчики"
- **Model Size on Photo**: 110
- **Model Height on Photo**: 110 см

### E. Logistics & Policy
- **Estimated Delivery**: "Завтра" (Tomorrow)
- **Warehouse Location**: "склад WB"
- **Return Policy**: "14 дней на возврат"
- **Fitting Available**: "Есть примерка"

---

## 2. Current Backend Field Inventory
Examining `ProductBaseEntity`, `ListingVariantEntity`, and `SkuEntity`, we currently have:

| Entity | Field | Status | Notes |
| :--- | :--- | :--- | :--- |
| **ProductBase** | `name` | Found | Maps to Product Name |
| **ProductBase** | `categoryName` | Found | Maps to Category/Brand partial |
| **ListingVariant** | `color` | Found | Maps to Color Key / Display Name |
| **ListingVariant** | `slug` | Found | Used for URL |
| **ListingVariant** | `webDescription`| Found | Textual description |
| **Sku** | `erpItemCode` | Found | Unique identifier per size |
| **Sku** | `sizeLabel` | Found | Maps to Size (92, 98, etc.) |
| **Sku** | `stockQuantity` | Found | Numeric stock count |
| **Sku** | `originalPrice` | Found | Maps to "Old Price" |
| **Discount** | `title` | Found | Maps to Promotion Title |
| **Discount** | `discountValue` | Found | Used to compute current price |

---

## 3. Missing Fields (Gaps)
The following fields are **missing** from the backend database schema and read models:

### 1. Social & Trust Metrics (High Priority)
- `rating` (BigDecimal): Average product rating.
- `reviewCount` (Integer): Total number of customer reviews.
- `questionsCount` (Integer): Number of user questions.
- `branded` (Boolean/String): "Original" or "Authentic" status flag.
- `brandId/brandName`: While `categoryName` is used, the UI shows a distinct Brand entity ("Baby Style") separate from navigation categories.

### 2. Product Attributes (Detailed)
- `articleNumber` (String): The "Артикул" (79653867) is missing at the variant level.
- `composition` (String): Structured field for material details.
- `gender` (Enum/String): Target audience (Boys/Girls/Unisex).
- `modelSize` (String): Size worn by the model in photos.
- `modelHeight` (String): Height of the model in photos.

### 3. Logistics & Badges
- `returnPolicy` (String): "14 days return" info.
- `fittingAvailable` (Boolean): Flag for "Has fitting room".
- `warehouseName` (String): "склад WB" or similar stock location label.
- `deliveryPromise` (String): Estimated delivery timeframe.

### 4. Pricing Details
- `referencePrice` (BigDecimal): The 4,444 P price (often RRP or MSRP) is not currently stored. Our system only tracks `originalPrice` and computes `discountedPrice`.

### 5. Seller Information
- `sellerName` (String): Information about the shop/merchant.
- `sellerRating` (BigDecimal): Trust score of the merchant.

---

## 4. Logical Placement Recommendations
To support these fields without cluttering the existing schema, the following additions are recommended:

1. **`ProductBaseEntity`**: Add `brand`, `gender`, and `composition` as these are usually shared across all colors.
2. **`ListingVariantEntity`**: Add `rating`, `reviewCount`, `questionsCount`, `articleNumber`, `modelSize`, and `modelHeight`.
3. **`SkuEntity`**: Add `referencePrice` (MSRP).
4. **New `SellerEntity`**: Link to `ProductBase` or `ListingVariant` to store Merchant data.
