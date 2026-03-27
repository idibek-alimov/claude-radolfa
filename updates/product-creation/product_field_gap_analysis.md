# Product Entity Gap Analysis vs Wildberries

## What We Have (Entity Scan Summary)

| Entity | Fields |
|---|---|
| `ProductBaseEntity` | id, externalRef, name, category, categoryName, variants |
| `ListingVariantEntity` | id, productBase, color, slug, productCode, webDescription, topSelling, featured, images (joined table), attributes (joined table), lastSyncAt, skus |
| `SkuEntity` | id, listingVariant, skuCode, sizeLabel, stockQuantity, originalPrice |
| `ListingVariantAttributeEntity` | id, listingVariant, attrKey, attrValue, sortOrder |
| `ListingVariantImageEntity` | (assumed: id, listingVariant, url, sortOrder) |
| `ColorEntity` | id, colorKey, displayName, hexCode |
| `CategoryEntity` | id, name, slug, parent, children |

---

## Gap Analysis: What is Missing (Compared to WB Checklist)

### HIGH PRIORITY (Block real marketplace operations)

| Missing Field | Where it Belongs | Why It Matters |
|---|---|---|
| **Brand** | `ProductBaseEntity` | Required by WB. Customers filter by brand. Already planned in Phase 7. |
| **Barcode** | `SkuEntity` | Required for warehouse/logistics. Every size-level SKU needs a barcode. WB requires this. Currently `skuCode` is internal. |
| **Weight (kg)** | `SkuEntity` | Required for shipping cost calculation. Without it, you can't charge accurate delivery. |
| **Package Dimensions** (W×H×D cm) | `SkuEntity` | Same as weight — required by any courier/warehouse integration. |

### MEDIUM PRIORITY (Improve search & data quality)

| Missing Field | Where it Belongs | Why It Matters |
|---|---|---|
| **Vendor Article Number (vendorCode)** | `ProductBaseEntity` | The internal stock-keeping reference for the seller/admin. Different from external ERP ref. |
| **Keywords/Tags** | `ListingVariantEntity` | Optional in WB but heavily improves search ranking in your own storefront search. |
| **Composition/Materials** | (can be an attribute blueprint) | WB-specific category field. Can be handled by Phase 6 Category Blueprints — no new column needed. |

### LOW PRIORITY / DEFER (Future Marketplace Scalability)

| Missing Feature | Note |
|---|---|
| **Product Owner / Seller** | For a multi-vendor marketplace. Not needed if only Radolfa sells. Defer to later. |
| **Warehouse / Storage Location** | Needed for fulfilment routing. Defer until you have real warehouse operations. |
| **Inventory Reservation / Locks** | Needed for preventing overselling at high concurrency. Defer until needed. |

---

## Recommendation on Warehouse / Owner

**Short answer: Do NOT add it now.**

**Reasoning:**
- You are building a single-seller marketplace (Radolfa owns all products). Adding a `seller_id` and `warehouse_id` column now just adds noise with no benefit.
- When you are ready to go multi-vendor, you add a `SellerEntity` and a `WarehouseEntity` and link them. This is a clean, well-understood migration that doesn't require rethinking your current design.
- The **one thing you SHOULD add now** is `weight` and `package_dimensions` on the SKU, because these affect shipping cost today, not just future multi-vendor scenarios.

**What to do instead:** Design the system cleanly *without* those concerns. A well-designed single-vendor system is much easier to extend to multi-vendor than a poorly-designed one that tries to anticipate everything.

---

## Prompt Updates Needed

Based on this analysis, the `claude_product_creation_prompt.md` should be updated to:
1. Add `weight` and `dimensions` fields to the `SkuEntity` and `SkuDefinitionDto` (HIGH priority).
2. Add `barcode` to `SkuEntity` (HIGH priority — critical for logistics).
3. Confirm that `tags`/`keywords` are added to `ListingVariantEntity` (MEDIUM).
