# ERPNext <-> Radolfa Sync Architecture Review

**Date:** February 2026
**Version:** 1.0
**Target System:** Radolfa (Java/Spring Boot Backend + Next.js Frontend)

---

## 1. Executive Summary

The proposed move from a **Flat SKU Structure** (current) to a **3-Tier Hierarchical Structure** (`ProductBase` -> `ListingVariant` -> `SKU`) is **correct and necessary** for a modern e-commerce experience ("Amazon-style").

Your proposed SQL schema is 90% there. The main engineering challenges will not be the database tables, but strictly mapping the loose, dynamic nature of ERPNext Variants into the strict, compiled Domain Models of Java/Spring Boot without creating a "distributed monolith" where a change in ERPNext breaks the WebApp.

This report details the specific architectural changes required for your Java/Spring Boot backend to support this.

---

## 2. Data Modeling Strategy (Backend)

### The 3-Tier Hierarchy
We need to refactor the current `Product.java` (which combines SKU and Marketing data) into three distinct entities.

#### Tier 1: Product Base (`product_bases`)
*   **Role**: Logical grouping. Holds shared attributes (e.g., "Cotton T-Shirt").
*   **Key Field**: `erp_template_code` (Matches ERPNext "Item Template").
*   **Java Model**: A lightweight Entity. Mostly used for grouping queries.

#### Tier 2: Listing Variant (`listing_variants`)
*   **Role**: **The Core "Product" Concept**. This is what receives a URL (`/products/t-shirt-red`), a description, and a set of images.
*   **Key Field**: `color_key` (Derived from ERP Attribute "Color").
*   **Ownership**:
    *   **Create**: Triggered by ERP.
    *   **Update**: **Product Owner (Marketing Team)**. Description and Images are master-data here.
*   **Images**: Moved here from the generic Product level.

#### Tier 3: SKU (`skus`)
*   **Role**: The "Inventory & Price" holder.
*   **Key Field**: `erp_item_code` (The specific Variant Item Code, e.g., `TSHIRT-RED-XL`).
*   **Data**: `size_label` (Derived from ERP Attribute "Size"), `stock_quantity`, `price`.
*   **Ownership**: **Strictly ERP**.

### Schema Recommendations (Refining your proposal)

Your SQL proposal was good. Here are specific refinements for Postgres + Spring Data JPA:

```sql
-- REFINEMENT: Add Optimistic Locking for Stock High-Frequency Updates
CREATE TABLE skus (
    id BIGSERIAL PRIMARY KEY,
    -- ... other fields ...
    version BIGINT DEFAULT 0, -- @Version in JPA
    last_stock_update TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_erp_code UNIQUE (erp_item_code)
);

-- REFINEMENT: Composite Keys for efficient lookups
CREATE INDEX idx_listing_slug ON listing_variants(slug);
CREATE INDEX idx_sku_lookup ON skus(erp_item_code);
```

---

## 3. The "Smart" Sync Architecture

The biggest risk in your plan is the "Server Script". If you try to do too much logic in Python (ERPNext side), you bind your ERP too tightly to your WebApp structure.

**Recommendation: The "Dumb Pipe" Approach**

Keep the ERPNext script simple. Send *Context*, not *Instructions*.

### payload A: The "Definition" Payload (Webhook)
Trigger: `Item` (Variant) creation or update.
Structure: Send the **Hierarchy** info in the payload.

```json
{
  "event": "item_update",
  "item_code": "TSHIRT-RED-XL",
  "item_name": "T-Shirt Red Extra Large",
  "variant_of": "TSHIRT-TEMPLATE",  // Crucial: The Parent
  "attributes": {
    "Color": "Red",
    "Size": "XL"
  }
}
```

### Server-Side Processing (Java)
Your Java Service (`SyncProductsService`) needs to handle the idempotency:
1.  **Incoming Payload**: `TSHIRT-RED-XL`
2.  **Lookup**: Does `ProductBase` "TSHIRT-TEMPLATE" exist?
    *   *No*: Create it.
    *   *Yes*: Load it.
3.  **Lookup**: Does `ListingVariant` for "Color: Red" exist under that Base?
    *   *No*: Create it. Initialize `slug` = `t-shirt-red`. Initialize `description` from Template.
    *   *Yes*: **Do nothing to description/images**. Only update sync timestamp.
4.  **Upsert**: The `SKU` for "Size: XL".
    *   Update Price/Stock.

### payload B: The "High-Speed" Stock Payload
Trigger: `Bin` on_update (Stock Level Change).
Optimized for speed.

```json
{
  "item_code": "TSHIRT-RED-XL",
  "qty": 50
}
```
**Java Handling**:
*   Use a custom JPQL query: `UPDATE skus SET stock_quantity = :qty WHERE erp_item_code = :code`.
*   Skip loading the full entity graph to save performance.

---

## 4. Specific Issues in Your Plan

1.  **"Description" Handling**:
    *   *Your Plan*: "Initial Sync populate, subsequent ignore."
    *   *Critique*: Good logic, but hard to implement if you don't track *who* changed it.
    *   *Better Approach*: distinct columns. `erp_description` (always overwritten by sink) vs `web_description` (never touched by sync). Frontend shows `web` if present, else falls back to `erp`. This allows you to "Reset" to ERP version if needed.

2.  **Image Handling**:
    *   *Your Plan*: "Images JSONB".
    *   *Critique*: In Spring Boot/JPA, `@ElementCollection` or a separate `listing_images` table is cleaner and type-safe compared to raw JSONB, unless you specifically need the flexibility. Given you already have a `product_images` table (from `current_entity_structure.md`), stick with a separate relational table keyed to `listing_variant_id`.

3.  **URL Strategy (SEO)**:
    *   Your plan mentions `slug` on the Listing Variant. This is correct.
    *   Example: `radolfa.tj/products/red-performance-tshirt`
    *   Ensure your `listing_variants` table has a `UNIQUE` constraint on `slug`.

---

## 5. Implementation Roadmap

1.  **Database Migration (Flyway)**:
    *   Rename `products` -> `product_bases` (or create new).
    *   Create `listing_variants` table.
    *   Create `skus` table.
    *   **Migration Step**: Map current "Flat" items to be their own Base+Variant+SKU (1:1:1) temporarily to avoid data loss.

2.  **Java Domain Refactor**:
    *   Create `ListingVariant.java` (Aggregate Root for Shop).
    *   Create `ProductBase.java`.
    *   Create `Sku.java`.
    *   Retire `Product.java` (or refactor it into `ProductBase`).

3.  **ERPNext Script Update**:
    *   Update Python script to include `variant_of` and `attributes` dict in the payload.

4.  **Frontend Update**:
    *   User loads "Product Page" -> Fetches `ListingVariant` + List of `SKUs`.
    *   UI: "Color" selector switches the *URL* (loads different Listing Variant).
    *   UI: "Size" selector just updates the "Buy" button state (Client-side logic from SKU list).

## 6. Score
*   **Architecture**: 9/10 (Solid standard approach).
*   **Feasibility**: 10/10.
*   **Complexity**: Moderate (Requires careful migration of existing data).
