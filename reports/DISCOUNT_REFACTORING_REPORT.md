# Backend Pricing & Discount Implementation Report

This report outlines the current state of the backend pricing and discount logic, specifically tailored to identify what needs to change to transition `DiscountEntity` from linking to a single item to a **One-to-Many** relationship (one discount linked to multiple products/SKUs) with additional UI attributes like `name` and `colorHex`.

---

## 1. Current Database Schema & Entities

### `DiscountEntity` (Table: `discounts`)
Currently, this table maps **1:1** to a specific SKU via the `itemCode` field.
- **Fields:** `id`, `erpPricingRuleId` (unique), `itemCode` (links to SkuEntity), `discountValue` (percentage), `validFrom`, `validUpto`, `disabled`.
- **Limitation for Refactor:** It holds `itemCode` directly. To make it One-to-Many and support named discounts, this needs to change.

### `SkuEntity` (Table: `skus`)
- Holds the bare-minimum pricing: `originalPrice`.
- **Note:** It does *not* store `discountedPrice` or `discountPercentage` in the database. These are computed dynamically at runtime.

### `ListingVariantEntity` (Table: `listing_variants`)
- Maps to multiple SKUs. Does not know about discounts directly.

---

## 2. Current Write/Sync Flow (ERP → DB)

Discounts are pushed to the backend from ERPNext via a webhook/API call.
- **Controller:** `ErpSyncController` handles `POST /api/v1/erp/discounts`.
- **Payload:** `SyncDiscountPayload` receives `erpPricingRuleId`, `itemCode`, `discountValue`, `validFrom`, `validUpto`.
- **Service:** `SyncDiscountUseCaseImpl` validates and passes it to the `DiscountAdapter`.
- **Persistence:** `DiscountAdapter` saves or updates the `DiscountEntity`, matching it exactly to one `itemCode`.

---

## 3. Current Read/Query Flow (DB → Frontend)

How do products get their `discountedPrice` when a user browses the site? It is dynamically calculated via a two-step process:

1. **Base Query:** `ListingVariantRepository` (e.g., `findGridPage`, `findDetailBySlug`) performs SQL queries that fetch the product data and aggregate the `MIN(s.originalPrice)` from `skus` table. *It does not query the discounts table.*
2. **Enrichment (`DiscountEnrichmentAdapter`):** 
   - Takes the results from Step 1, extracts all `erpItemCode`s.
   - Queries `DiscountRepository` for active discounts matching those specific `itemCodes`.
   - Math: Automatically computes `discountedPrice` = `originalPrice * (1 - discountValue / 100)`.
   - If a variant has 5 SKUs and 3 of them are on sale, it finds the cheapest computed `discountedPrice` and assigns that to the `ListingVariantDto` so the frontend can display the cheapest starting price.

*(Note: Loyalty tier discounts are applied *after* this step by `TierPricingEnricher`, based on the current logged-in user).*

---

## 4. Required Changes for the "One-to-Many" Refactor

To achieve the goal of naming discounts (e.g., "FallDiscount"), giving them colours, and linking one discount to multiple products:

### A. Database / Entity Changes
1. **Remove `itemCode`** from `DiscountEntity`.
2. **Add new fields** to `DiscountEntity`: `name` (String) and `colorHexCode` (String).
3. **Create the One-to-Many Link:**
   - **Option 1 (Join Table - Recommended):** Create a `@ManyToMany` mapping table (e.g., `discount_items`) linking `discount_id` to multiple `sku_id`s or `erp_item_code`s.
   - **Option 2 (Foreign Key on SKU):** Add a `discount_id` foreign key column to the `skus` table. *(This limits a SKU to only being in 1 discount at a time, which is usually standard).*

### B. ERP Sync API Changes
1. Update `SyncDiscountPayload` in `ErpSyncController` to receive the new fields from ERPNext:
   - `name` (e.g., "FallDiscount")
   - `colorHexCode` (e.g., "#FF5733")
   - `itemCodes` (List of Strings, instead of a single `itemCode`)
2. Update `SyncDiscountUseCase` and `DiscountAdapter` to save the parent `DiscountEntity`, and then update the relationships (saving to the join table or updating the FKs on the SKUs).

### C. Read Flow Enrichment Changes
1. Update `DiscountEnrichmentAdapter`. Instead of doing `findActiveByItemCodes()`, you will need to map which `itemCode` belongs to which active `DiscountEntity` based on the new one-to-many relationship.
2. Ensure `ListingVariantDto`, `ListingVariantDetailDto`, and `SkuDto` are updated to return the new `saleName` and `saleColorHex` fields so the frontend can render the beautiful badges.
