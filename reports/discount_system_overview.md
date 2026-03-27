# Backend Discount System Overview

This report provides a technical overview of how discounts are implemented in the Radolfa backend, covering the data model, synchronization from ERPNext, and runtime application logic.

## 1. Data Model
Discounts are primarily managed through the `DiscountEntity`, which represents an **ERP Pricing Rule**.

### Key Entities
- **`DiscountEntity`**: Stores the core rules (value, validity dates, status) and UI attributes (`title`, `colorHex`).
- **`discount_items`**: A join table (ElementCollection) linking a single discount to one or more `erp_item_code`s (SKUs). This allows a "One-to-Many" relationship where one rule applies to several products.

### Attributes
- `erpPricingRuleId`: Unique identifier from ERPNext.
- `discountValue`: Percentage to be subtracted (e.g., `10.00` for 10% off).
- `validFrom` / `validUpto`: The time window during which the discount is active.
- `title` / `colorHex`: UI-specific metadata used for rendering badges on the frontend.

---

## 2. Synchronization (ERP → Backend)
Discounts are pushed to the backend via the `ErpSyncController` (`POST /api/v1/erp/discounts`).

1. **Webhook Reception**: ERPNext triggers a webhook whenever a Pricing Rule is created/updated.
2. **`SyncDiscountService`**: 
   - Handles the upsert logic.
   - If a discount with the same `erpPricingRuleId` exists, it is updated.
   - It maps a list of `itemCodes` to the discount, ensuring the backend stays in sync with ERPNext's multi-item rules.
3. **Deletions**: Handled via `DELETE /api/v1/erp/discounts/{erpPricingRuleId}`.

---

## 3. Runtime Price Calculation
Discounts are **not** persisted on the product or SKU records. They are calculated dynamically during the "read flow" (when fetching product listings or details).

### A. General Discounts (`DiscountEnrichmentAdapter`)
When products are fetched (e.g., for the homepage or search results):
1. The `ListingReadAdapter` fetches the basic product/SKU data.
2. The `DiscountEnrichmentAdapter` is called to "enrich" this data.
3. It queries all active discounts for the relevant `itemCodes`.
4. **Best Discount Logic**: If multiple discounts apply to the same SKU, the one with the highest `discountValue` is chosen.
5. **Variant Logic**: For a product with multiple variants/SKUs, the system identifies the SKU that results in the **lowest effective price** and uses that for the main listing card.

### B. Loyalty Tier Discounts (`TierPricingEnricher`)
Personalized discounts based on the user's loyalty tier are applied on top of general discounts:
1. This is a request-scoped component.
2. It identifies the authenticated user and resolves their loyalty tier percentage via `ResolveUserDiscountService`.
3. It populates the `loyaltyPrice` and `loyaltyDiscountPercentage` fields in the DTO, which the frontend uses to show "Member Price" or "Gold Tier Price".

---

## 4. UI/UX Integration
The backend serves specific fields to help the frontend render beautiful discount badges:
- `discountedPrice`: The final price after the best general discount.
- `saleTitle`: The name of the campaign (e.g., "Holiday Sale").
- `saleColorHex`: A custom color for the badge (e.g., `#FF0000` for red).

---

## 5. Performance Strategy
- **Batch Loading**: Discounts are loaded using a single query for all items on a page (using `IN` clause with item codes), avoiding the N+1 problem.
- **Indexing**: The `erp_pricing_rule_id` is indexed, and the `discount_items` table is joined efficiently in JPA queries.
- **Lazy Enrichment**: Pricing enrichment happens at the very edge of the service layer (in the Adapter/DTO mapping phase), keeping the core domain models clean and focused on business logic.
