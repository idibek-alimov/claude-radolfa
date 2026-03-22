# Radolfa Backend Product Creation: Multi-Phased Implementation Plan

> **Development Phase Notice:** This plan is executed during an **early development phase with no production data**. Dropping columns, recreating tables, altering constraints, and replacing migrations is explicitly permitted. Do not treat any schema change as risky.

## Architectural Goal (Inspired by Wildberries)
We are refactoring the product creation flow in the Radolfa backend to support a robust, multi-variant hierarchy in a single pass.
The new structure follows the **Base -> Color Variants -> Sizes** model:
1.  **Product Base (Nomenclature):** Contains the overarching Category, Brand, and Title.
2.  **Color Variants (Listing Variants):** Contains the Color, Description, Attributes (dynamic key-values), and an array of Image URLs attached to this specific color.
3.  **Sizes (SKUs):** Under each Color Variant, each size gets its own SKU, Barcode, Stock, and Price.

*Note: Marketing flags (`topSelling` and `featured`) are explicitly removed from creation and are strictly post-creation/admin updates.*
*Note 2: Image uploading is handled via a separate endpoint. The frontend will upload the physical files first, get the URLs, and then pass an array of `images` (URLs) to this product creation API.*

---

## Execution Tracker
> **Instructions for Claude Code:** Execute this plan ONE phase at a time. After completing a phase, update this table below to change the status to `✅ COMPLETE` and add a concise 1-sentence summary of what was implemented. **Do not proceed to the next phase until the user reviews the current one.**

| Phase | Status | Summary of Implementation |
|---|---|---|
| **Phase 0:** Media Infrastructure | ✅ COMPLETE | Added `GenericUploadImageUseCase` + `GenericUploadImageService` and `POST /api/v1/admin/images/upload` endpoint — uploads to `uploads/media/{uuid}.ext` with no product context required. |
| **Phase 1:** Update Presentation DTOs + Schema | ✅ COMPLETE | Restructured `CreateProductRequestDto` to accept `List<ListingVariantCreationDto>` (with nested `ProductAttributeDto` + expanded `SkuDefinitionDto`); added barcode/weight/dimensions to `SkuEntity` and `V7` Flyway migration. |
| **Phase 2:** Refactor Domain & Constraints | ✅ COMPLETE | Replaced `Command.colorId+skus` with `List<VariantDefinition>` (holding colorId, webDescription, attributes, images, skus); expanded `SkuDefinition` with 5 logistics fields; added matching fields + full constructor to `Sku` domain model (legacy 6-arg constructor preserved for ERP sync path). |
| **Phase 3:** Multi-Variant Creation | ✅ COMPLETE | Refactored `CreateProductService` to iterate `command.variants()` within one transaction — resolves color, applies webDescription/attributes/images, creates SKUs with logistics fields per variant; fixed controller mapping from new DTO shape to new Command shape. Build compiles clean. |
| **Phase 4:** Post-Creation Attribute Editing | ✅ COMPLETE | Added `List<ProductAttribute> attributes` to `UpdateListingCommand`; service applies `setAttributes()` when present; controller exposes `ProductAttributeDto` on `UpdateListingRequest` and maps to domain type. Build compiles clean. |
| **Phase 5:** Mapper & Persistence Verification | ⏳ PENDING | |
| **Phase 6:** Brand Integration | ⏳ PENDING | |
| **Phase 7:** Category-Attribute Blueprints | ⏳ PENDING | |
| **Phase 8:** Final Cleanup | ⏳ PENDING | |

---

## Phase 0: Media Infrastructure (Generic Upload)
**Goal: Introduce a "staged upload" endpoint that returns a URL WITHOUT requiring a product slug.**
*   **New In-Port:** Create `GenericUploadImageUseCase.java` for uploading images without context.
*   **New Service:** Create `GenericUploadImageService.java` that processes and uploads to a generic `uploads/media/` S3 path (we use a permanent media path rather than a 'temp' path to avoid needing an async file move later when the product is saved).
*   **Controller:** In `ProductManagementController.java` (or a dedicated media admin controller) add `POST /api/v1/admin/images/upload` secured for `MANAGER` or `ADMIN`. *(Do not put this in ListingController since its base path is /api/v1/listings).*


## Phase 1: Update Presentation Layer (DTOs) + Schema Migration
**Goal: Restructure the API payload to accept multiple variants at once, AND apply all schema changes needed by Phases 2–3 upfront so there are no compile/runtime blockers downstream.**

### 1a — DTO Restructure
*   **File:** `CreateProductRequestDto.java`
    *   Remove `colorId` and `skus` from the root level. *(Note: Ensure `name` and `categoryId` are preserved at the root level, as they are required to create the Product Base).*
    *   Add a new nested record (e.g., `ListingVariantCreationDto`) inside the DTO containing:
        *   `@NotNull Long colorId`
        *   `String webDescription`
        *   `List<ProductAttributeDto> attributes` (create `ProductAttributeDto(String key, String value, int sortOrder)`)
        *   `List<String> images` (URLs)
        *   `@NotEmpty List<SkuDefinitionDto> skus` — where `SkuDefinitionDto` must now include:
            *   `String sizeLabel`
            *   `BigDecimal price`
            *   `int stockQuantity`
            *   `String barcode` (unique per SKU — required for logistics)
            *   `Double weightKg` (optional — used for shipping calculation)
            *   `Integer widthCm`, `Integer heightCm`, `Integer depthCm` (optional — package dimensions)
    *   Add `@NotEmpty List<ListingVariantCreationDto> variants` to the root of `CreateProductRequestDto`.

### 1b — Schema Migration (do this in the same phase, not deferred)
*   **File:** `SkuEntity.java`
    *   Add fields: `barcode` (String, unique, nullable for now), `weightKg` (Double), `widthCm` (Integer), `heightCm` (Integer), `depthCm` (Integer).
*   **Migration:** Create a Flyway migration that adds the above columns to the `sku` table. Add a unique constraint on `barcode`. Since this is a development environment, dropping and re-adding the column (or `ALTER TABLE ADD COLUMN`) is both acceptable approaches.


## Phase 2: Refactor the Domain & Use Case Constraints
**Goal: Propagate the payload changes through the interface ports.**
*   **File:** `CreateProductUseCase.java` (Interface & Command)
    *   Update `CreateProductUseCase.Command` to replace the single `colorId` and `skus` with a list of `VariantDefinition` internal records.
    *   Ensure each `VariantDefinition` holds `colorId`, `webDescription`, `attributes`, `images`, and the `List<SkuDefinition>`.
    *   Each `SkuDefinition` must include the new fields: `barcode`, `weightKg`, `widthCm`, `heightCm`, `depthCm`.
*   **File:** `Sku.java` (Domain model)
    *   Add the new fields to the `Sku` domain record/class: `barcode`, `weightKg`, `widthCm`, `heightCm`, `depthCm`.


## Phase 3: Implement the Multi-Variant Creation Logic
**Goal: Implement the core service logic to iterate and generate the variant hierarchy.**
*   **File:** Implementation of `CreateProductUseCase` (`CreateProductService.java`)
    *   When creating the `ProductBase`, iterate over the `Command.variants` list within a single `@Transactional` boundary.
    *   For **each** variant in the list, create the `ListingVariant` domain object. Make sure to preserve any existing logic that resolves the `colorId` into a `colorKey` via the Colour port.
    *   Call the enrichment methods on the `ListingVariant` to set the new fields:
        *   `updateWebDescription(variant.webDescription())`
        *   Iterate through images and call `addImage(url)` — images here are plain URLs passed in the request; no S3 upload happens during creation. S3 upload happens via the Phase 0 endpoint before the creation request is sent.
        *   Map `ProductAttributeDto`s to domain `ProductAttribute`s and call `setAttributes(...)`.
    *   Inside the variant loop, process the `SkuDefinition` list as done previously to generate the SKUs for that specific color+size matrix, including the new `barcode`/`weightKg`/dimension fields.
    *   Elasticsearch indexing remains fire-and-forget (as currently implemented) — it is outside the transaction boundary by design.


## Phase 4: Enable Post-Creation Attribute Editing
**Goal: The current update flow (`PUT /api/v1/listings/{slug}`) does not expose attributes. We need to allow managers to fix or update attributes after creation.**
*   **File:** `ListingController.java`
    *   Update `UpdateListingRequest` to include `List<ProductAttributeDto> attributes`.
*   **File:** `UpdateListingUseCase.java` & Implementation
    *   Update `UpdateListingCommand` to include the attributes list.
    *   In the service implementation, map and call `setAttributes(...)` on the `ListingVariant` alongside the existing `webDescription` update.


## Phase 5: Mapper & Persistence Verification
**Goal: Verify that all layers are wired end-to-end with the new fields. No new business logic — this is a cross-cutting verification pass.**
*   **File:** `ProductHierarchyMapper.java` (MapStruct)
    *   Update `SkuEntity` ↔ `Sku` mapping to include `barcode`, `weightKg`, `widthCm`, `heightCm`, `depthCm`.
    *   Verify `ListingVariantEntity` ↔ `ListingVariant` mapping still correctly handles `ListingVariantAttributeEntity` ↔ `ProductAttribute` (no changes expected, but confirm).
*   **Persistence check:** Confirm `ListingVariantEntity.java` persists the `ProductAttribute` list via `ListingVariantAttributeEntity` (joined table, no changes expected — verify only).
*   **Integration smoke test:** Create a product with 2 variants, 2 SKUs each, confirm all fields (barcode, images, attributes) persist and are returned correctly by `GET /api/v1/listings/{slug}`.


## Phase 6: Brand Integration
**Goal: Every professional product needs a Brand. The current `ProductBase` is missing it.**
*   **Entity:** Create `BrandEntity` (id, name, logo_url). Add a Flyway migration.
*   **Persistence:** Add a `@ManyToOne BrandEntity brand` (nullable, lazy) relationship to `ProductBaseEntity`. Add the FK column via migration.
*   **Domain:** Add optional `Long brandId` to the `ProductBase` domain model.
*   **DTO:** Add `Long brandId` (optional) to the root of `CreateProductRequestDto`.
*   **Logic:** Resolve `brandId` → `BrandEntity` in the creation service and attach to `ProductBase`.
*   **Important — sync boundary:** `brand` is a **Radolfa-managed field only**. It must NOT be overwritten by the ERP sync path (`applyExternalUpdate()`). Ensure `applyExternalUpdate()` in `ProductBase.java` does not touch `brandId`.


## Phase 7: Category-Attribute Blueprints (Wildberries Style)
> **Note:** This phase is **functionally independent** from Phases 1–6. It adds a new capability (category-driven attribute hints) and does not modify any existing creation or update flow. It can be deferred or executed as a standalone initiative after Phase 6 ships.

**Goal: Implement "Blueprints" so the frontend knows which attributes to show based on the selected category.**
*   **Entity:** Create `CategoryAttributeBlueprintEntity` (id, category_id FK, attribute_key, is_required, sort_order). Add a Flyway migration.
*   **API:** Create a read-endpoint `GET /api/v1/categories/{id}/blueprint` that returns the list of allowed/required attributes for that category.
*   **Validation:** (Optional/Bonus) In `CreateProductService`, validate that attributes sent in the creation request satisfy all `is_required = true` entries of the category's blueprint.


## Phase 8: Final Cleanup & OpenAPI Documentation
*   Confirm every new entity introduced (BrandEntity, CategoryAttributeBlueprintEntity) has a Flyway migration that ran cleanly.
*   Verify that the `POST /api/v1/admin/images/upload` endpoint from Phase 0 is correctly annotated with `@Operation` / `@ApiResponse` for Swagger/OpenAPI.
*   Review all new endpoints for consistent error response shapes (400/404/409 where applicable).
*   Remove any dead code left over from the old single-variant creation path.

---

### Persistence & Schema Instructions for Claude Code:
**This is an early development phase with no production data and no risk of data loss.** Do not pause or ask for permission regarding database schema changes. If you need to drop/recreate tables, alter columns, change entity mappings, or replace existing Flyway migrations — just do it and update the schema automatically. Treat all schema operations as zero-risk.
