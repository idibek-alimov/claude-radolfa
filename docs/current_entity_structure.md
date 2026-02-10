# Current Entity Structure Report

This document details the current implementation of core entities (Products and Users) in the Radolfa backend as of February 2026.

## 1. Product Entity

The Product entity is currently designed as a "Flat Structure" where each record in the database represents a unique SKU (linked to an ERP identifier).

### Database Schema (`products` table)

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `BIGSERIAL` | Internal primary key. |
| `erp_id` | `VARCHAR(64)` | Unique SKU identifier from ERPNext (e.g., `TSHIRT-RED-XL`). |
| `name` | `VARCHAR(255)` | Product name synced from ERP. |
| `price` | `NUMERIC(12,2)`| Current selling price from ERP. |
| `stock` | `INTEGER` | Physical stock quantity from ERP. |
| `web_description` | `TEXT` | Marketing description managed within Radolfa. |
| `is_top_selling` | `BOOLEAN` | Controls visibility in "Top Sellers" sections. |
| `created_at` | `TIMESTAMPTZ` | Record creation timestamp. |

### Normalized Images (`product_images` table)

Images are stored in a separate table for better scalability and metadata support.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `BIGSERIAL` | Primary key. |
| `product_id` | `BIGINT` | Foreign key to `products(id)`. |
| `image_url` | `TEXT` | URL to the image (e.g., S3). |
| `sort_order` | `INT` | Controls the display order of images. |

### Domain Model (`Product.java`)

The domain model follows a strict separation between **ERP-Locked** fields and **Radolfa-Owned** (Enrichment) fields.

- **ERP-Locked**: `name`, `price`, `stock`. These are updated only via the `enrichWithErpData` method during sync.
- **Radolfa-Owned**: `webDescription`, `topSelling`, `images`. These are managed by internal content teams.

---

## 2. User Entity

The User entity handles authentication and role-based access control.

### Database Schema (`users` table)

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `BIGSERIAL` | Primary key. |
| `phone` | `VARCHAR(32)` | Unique phone number (Primary Auth Identifer). |
| `role` | `VARCHAR(16)` | Enum: `USER`, `MANAGER`, `SYSTEM`. |
| `created_at` | `TIMESTAMPTZ` | Registration timestamp. |

---

## 3. Current Sync Logic

The system currently processes sync events as independent "SKU Updates":
1. The ERP sends a payload with `erp_id`, `name`, `price`, and `stock`.
2. The `SyncErpProductService` looks for an existing product with that `erp_id`.
3. If found, it updates the stock/price.
4. If NOT found, it creates a new product row.

> [!NOTE]
> Under the current architecture, a "Red XL T-Shirt" and a "Red L T-Shirt" are treated as two entirely unrelated products in the database and frontend.
