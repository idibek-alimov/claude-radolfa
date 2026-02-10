# Role and Objective
You are a Senior Backend Engineer (Spring Boot).
Your task is to **expose the newly created 3-Tier Product Hierarchy** to the frontend via REST APIs.

**Context:**
We recently refactored the backend from a "Flat Product" to a 3-Tier Model:
1.  `ProductBase` (Template)
2.  `ListingVariant` (Color Variant - *The Product Card*)
3.  `Sku` (Size/Stock - *The Purchasing Unit*)

**Goal:**
Enable a generic e-commerce frontend (like Amazon) to:
1.  Display "Color Cards" on the listing page (e.g., "T-Shirt (Red)", "T-Shirt (Blue)").
2.  Display "Size Options" with real-time stock on the detail page.

---

# 🛑 INTERACTIVE PROCESS (MANDATORY)

1.  **READ & RATE**: Read this prompt. Rate the API design.
2.  **PROPOSE PLAN**: List files to create/modify.
3.  **WAIT**: Ask for user confirmation.
4.  **EXECUTE**: Code only after approval.

---

# 1. New DTOs (Data Transfer Objects)

Create these in `infrastructure/web/dto/`.

### A. `SkuDto`
Represents a purchasable unit (Size).
*   `id` (Long)
*   `erpItemCode` (String)
*   `sizeLabel` (String) - e.g., "XL", "42"
*   `stockQuantity` (Integer)
*   `price` (BigDecimal) - Original Price
*   `salePrice` (BigDecimal) - Effective Price
*   `isOnSale` (boolean) - Calculated field
*   `saleEndsAt` (Instant)

### B. `ListingVariantDto` (The "Card")
Represents the product as seen on the search/home page.
*   `id` (Long)
*   `slug` (String) - e.g., "tshirt-red"
*   `name` (String) - From ProductBase name
*   `colorKey` (String) - e.g., "red"
*   `webDescription` (String)
*   `images` (List<String>)
*   `priceStart` (BigDecimal) - Lowest salePrice among all SKUs
*   `priceEnd` (BigDecimal) - Highest salePrice (if range)
*   `totalStock` (Integer) - Sum of all SKU stock

### C. `ListingVariantDetailDto` (The "Detail Page")
Extends `ListingVariantDto`.
*   `skus` (List<SkuDto>) - Full list of available sizes/stock.

---

# 2. Controller Implementation
**Create:** `infrastructure/web/ListingController.java`

*   **Endpoint 1: Get Catalog (Grid)**
    *   `GET /api/v1/listings`
    *   **Logic**: Fetch `ListingVariants`.
    *   **Response**: `PageResult<ListingVariantDto>`

*   **Endpoint 2: Get Product Detail**
    *   `GET /api/v1/listings/{slug}`
    *   **Logic**: Fetch a specific `ListingVariant` by slug.
    *   **Response**: `ListingVariantDetailDto` (Must include ALL SKUs for that variant).
    *   **Error**: 404 if not found.

*   **Endpoint 3: Search (Update)**
    *   `GET /api/v1/listings/search?q={query}`
    *   Refactor the existing search logic to return `ListingVariantDto` instead of the old `ProductDto`.

---

# 3. Domain Service Updates
You may need to create or update a Use Case to assemble this data.

*   **Create:** `application/services/GetListingService.java`
    *   Implements `GetListingUseCase` (Create this port too).
    *   Methods:
        *   `getPage(page, limit)`
        *   `getBySlug(slug)` -> returns Variant + loaded SKUs.

---

# 4. Constraints
*   **ReadOnly**: These endpoints are public and read-only.
*   **Performance**: When fetching the "Grid" (Endpoint 1), do **NOT** fetch all SKUs for every product if possible. However, you need the aggregate "Price From" and "Total Stock".
    *   *Tip*: You might need a custom JPQL query in `ListingVariantRepository` to fetch these aggregates efficiently.
*   **Tests**: Create a simple Integration Test for `ListingController` ensures the slug resolution works.
