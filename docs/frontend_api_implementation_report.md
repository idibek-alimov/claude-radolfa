# Frontend API Report: 3-Tier Product Hierarchy

**Status:** Implementation Verified
**Goal:** Empower the frontend to display products like Amazon (Separate Color Cards & Size Selectors).

---

## 1. The New Entry Point: `ListingController`
All storefront (public) operations have been consolidated into `ListingController.java`. This replaces the legacy search/list logic with a hierarchical-aware API.

### Key Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **GET** | `/api/v1/listings` | Paginated "Grid" view (Color Cards). |
| **GET** | `/api/v1/listings/{slug}` | Detail Page view (Full data + Sizes/Stock). |
| **GET** | `/api/v1/listings/search` | Search results based on Color Variants. |
| **GET** | `/api/v1/listings/autocomplete` | Real-time name suggestions. |

---

## 2. Updated Data Transfer Objects (DTOs)

The implementation uses three specific DTOs to balance performance and functionality.

### A. The "Product Card" (`ListingVariantDto`)
Used for the home page and category grid.
*   **Key Fields**:
    *   `slug`: The unique URL handle (e.g., `iphone-15-black`).
    *   `priceStart` / `priceEnd`: Aggregated price range from all SKUs (Size options).
    *   `totalStock`: Combined stock of all sizes.
    *   `colorKey`: Used for rendering swatches or labels.

### B. The "Product Detail" (`ListingVariantDetailDto`)
Used for the individual product page.
*   **Key Fields**:
    *   `skus`: List of all available sizes and their specific stock/prices.
    *   `siblingVariants`: A collection of other colors for the same product (enables "Color Swatches").
*   **SiblingVariant Structure**:
    *   `slug`: URL to the other color.
    *   `colorKey`: The color identifier.
    *   `thumbnail`: First image of that color variant.

### C. The "Buying Unit" (`SkuDto`)
Nested within the detail DTO.
*   **Key Fields**:
    *   `sizeLabel`: (e.g., "M", "L", "42").
    *   `stockQuantity`: Exact quantity in warehouse.
    *   `price` / `salePrice`: Supports strikethrough pricing.
    *   `onSale`: Boolean flag for UI badges.

---

## 3. How the Frontend Uses This (User Flow)

### Step 1: Browsing the Storefront
The frontend calls `GET /api/v1/listings`. 
*   **Result**: The user sees one card per color (e.g., "Winter Jacket — Navy" and "Winter Jacket — Black").

### Step 2: Selecting a Product
The user clicks the "Navy" jacket card. The frontend navigates to `/[slug]`.

### Step 3: Landing on the Detail Page
The frontend calls `GET /api/v1/listings/winter-jacket-navy`.
*   **Response**: The UI renders the Navy images and the navy description.
*   **Color Swatches**: The UI loops through `siblingVariants` to show a "Black" swatch link.
*   **Size Selector**: The UI loops through `skus` to populate the size dropdown.
    *   *Real-time*: If "Size XL" has `stockQuantity: 0`, the UI can immediately disable that button.

---

## 4. Legacy Management
The `ProductController.java` still exists but is now annotated as **Legacy**. It is reserved for administrative full-CRUD operations and does not directly serve the storefront's hierarchical needs.

---

**Summary**: Your backend is now perfectly aligned with high-end e-commerce standards. The API provides exactly what the frontend needs to render a rich, interactive product catalog while remaining efficient by aggregating data on the grid view.
