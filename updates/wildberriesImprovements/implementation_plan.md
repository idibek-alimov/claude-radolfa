# Radolfa — Wildberries-Inspired Catalog Improvements: Multi-Phase Plan

> **Development Phase Notice:** This plan is executed during an **early development phase with no production data**. Dropping columns, recreating tables, altering constraints, and replacing Flyway migrations is explicitly permitted. Do not treat any schema change as risky.

## Goals

This plan implements four structural improvements to the product catalog inspired by Wildberries' production architecture:

| # | Improvement | What changes |
|---|---|---|
| P1 | **Typed Attribute Blueprints** | `CategoryAttributeBlueprintEntity` gains `type`, `unitName`, and `allowedValues` — the frontend can render correct input controls per attribute |
| P2 | **Multi-Value Attributes** | `ProductAttribute.value: String` → `values: List<String>` — a product can express "Cotton 50% + Acrylic 50%" as a single attribute |
| P3 | **Tags System** | Hard-coded `topSelling`/`featured` booleans are replaced with a flexible `ProductTag` entity — marketing labels without schema migrations |
| P4 | **Dimensions at Variant Level** | `weightKg, widthCm, heightCm, depthCm` move from `Sku` → `ListingVariant` — one logistics profile per color, not per size |

---

## Execution Tracker

> **Instructions for Claude Code:** Execute this plan ONE phase at a time. After completing a phase, update this table to change the status to `✅ COMPLETE` and add a concise 1-sentence summary. **Do not proceed to the next phase until the user reviews the current one.**

| Phase | Status | Summary of Implementation |
|---|---|---|
| **Phase 1:** Blueprint Type System | ⬜ PENDING | |
| **Phase 2:** Multi-Value Attribute Storage | ⬜ PENDING | |
| **Phase 3:** Tags System | ⬜ PENDING | |
| **Phase 4:** Dimensions to Variant Level | ⬜ PENDING | |
| **Phase 5:** Blueprint Validation + Final Cleanup | ⬜ PENDING | |

---

## Phase 1: Blueprint Type System

**Goal: Upgrade `CategoryAttributeBlueprintEntity` to carry full type metadata — type, unit name, and an ordered list of allowed values for ENUM/MULTI attributes.**

### 1a — Domain: New `AttributeType` Enum

- **File:** Create `tj/radolfa/domain/model/AttributeType.java`
  - Pure Java enum: `TEXT`, `NUMBER`, `ENUM`, `MULTI`
    - `TEXT` — freeform single-line string (e.g., "Season: Summer")
    - `NUMBER` — numeric value, unit displayed via `unitName` (e.g., "Weight: 200 gr")
    - `ENUM` — single selection from a fixed list (e.g., "Fit: Oversized")
    - `MULTI` — multiple selections from a fixed list (e.g., "Material: Cotton, Acrylic")
  - Place in the `domain` package — it is pure Java with zero external dependencies.

### 1b — Schema Migration

- Create a new Flyway migration file (next available version number).
- **Alter `category_attribute_blueprints`:**
  - Add column `type VARCHAR(16) NOT NULL DEFAULT 'TEXT'` — stores the `AttributeType` enum name.
  - Add column `unit_name VARCHAR(64)` — nullable, display label appended to values (e.g., "cm", "kg").
- **Create new table `category_attribute_blueprint_values`:**
  ```sql
  CREATE TABLE category_attribute_blueprint_values (
      id          BIGSERIAL PRIMARY KEY,
      blueprint_id BIGINT NOT NULL REFERENCES category_attribute_blueprints(id) ON DELETE CASCADE,
      allowed_value VARCHAR(256) NOT NULL,
      sort_order  INT NOT NULL DEFAULT 0
  );
  ```
  This table stores the finite list of valid values for `ENUM` and `MULTI` type blueprints.

### 1c — JPA Entities

- **File:** `CategoryAttributeBlueprintEntity.java`
  - Add `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private AttributeType type;`
  - Add `@Column(length = 64) private String unitName;`
  - Add a `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)` relationship to `CategoryAttributeBlueprintValueEntity`, ordered by `sort_order`.

- **File:** Create `CategoryAttributeBlueprintValueEntity.java`
  - Table: `category_attribute_blueprint_values`
  - Fields: `id: Long`, `blueprint: CategoryAttributeBlueprintEntity` (ManyToOne, lazy), `allowedValue: String`, `sortOrder: int`
  - Annotate with `@BatchSize(size = 50)`.

### 1d — Port & Read Model

- **File:** `LoadCategoryBlueprintPort.java`
  - Update the `BlueprintEntry` record to include:
    ```java
    record BlueprintEntry(
        Long id,
        Long categoryId,
        String attributeKey,
        AttributeType type,        // NEW
        String unitName,           // NEW — nullable
        List<String> allowedValues, // NEW — empty list for TEXT/NUMBER types
        boolean required,
        int sortOrder
    )
    ```

- **File:** `CategoryBlueprintAdapter.java` (persistence adapter)
  - Update the mapping from `CategoryAttributeBlueprintEntity` → `BlueprintEntry` to populate the three new fields.
  - `allowedValues` maps from `entity.getAllowedValues().stream().map(...allowedValue).toList()`.

### 1e — API Response

- **File:** Update the blueprint response DTO exposed by `GET /api/v1/categories/{id}/blueprint`.
  - Add `type: AttributeType`, `unitName: String`, `allowedValues: List<String>` to the response record/DTO.
  - The controller maps `BlueprintEntry` fields directly to the response.

### 1f — Blueprint Management API (NEW)

- **File:** Create `CategoryBlueprintManagementController.java` (or add to existing category management controller).
  - `POST /api/v1/admin/categories/{categoryId}/blueprint` — create a blueprint entry. Secured `ADMIN` only.
    - Request body: `{ "attributeKey": "Material", "type": "MULTI", "unitName": null, "allowedValues": ["Cotton", "Polyester", "Wool"], "required": true, "sortOrder": 0 }`
  - `DELETE /api/v1/admin/categories/{categoryId}/blueprint/{blueprintId}` — delete a blueprint entry. Secured `ADMIN` only.
  - Add corresponding `CreateCategoryBlueprintUseCase` and `DeleteCategoryBlueprintUseCase` in-ports plus service implementations.

---

## Phase 2: Multi-Value Attribute Storage

**Goal: Change `ProductAttribute.value: String` → `values: List<String>` across the entire stack — domain, persistence, DTOs, and mappers.**

### 2a — Schema Migration

- Create a new Flyway migration file.
- **Drop and recreate `listing_variant_attributes`:**
  ```sql
  DROP TABLE IF EXISTS listing_variant_attributes;

  CREATE TABLE listing_variant_attributes (
      id            BIGSERIAL PRIMARY KEY,
      listing_variant_id BIGINT NOT NULL REFERENCES listing_variants(id) ON DELETE CASCADE,
      attr_key      VARCHAR(128) NOT NULL,
      sort_order    INT NOT NULL DEFAULT 0
  );

  CREATE TABLE listing_variant_attribute_values (
      id           BIGSERIAL PRIMARY KEY,
      attribute_id BIGINT NOT NULL REFERENCES listing_variant_attributes(id) ON DELETE CASCADE,
      value        VARCHAR(512) NOT NULL,
      sort_order   INT NOT NULL DEFAULT 0
  );
  ```
  The old `attr_value` column is eliminated. Values now live in a separate child table.

### 2b — Domain Model

- **File:** `ProductAttribute.java` (domain record)
  - Change `String value` → `List<String> values`.
  - Update the validation constructor: check `values` is not null and not empty, and each element is not blank.
  - Update `sortOrder` field name if inconsistent — keep as is.
  - Example result: `record ProductAttribute(String key, List<String> values, int sortOrder)`

### 2c — JPA Entities

- **File:** `ListingVariantAttributeEntity.java`
  - Remove `attrValue: String`.
  - Add `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)` to a new `List<ListingVariantAttributeValueEntity> values`, ordered by `sort_order`.

- **File:** Create `ListingVariantAttributeValueEntity.java`
  - Table: `listing_variant_attribute_values`
  - Fields: `id: Long`, `attribute: ListingVariantAttributeEntity` (ManyToOne, lazy), `value: String`, `sortOrder: int`

### 2d — DTOs

- **File:** `ProductAttributeDto.java`
  - Change `String value` → `List<String> values`.
  - Update `@NotBlank` validation: replace with `@NotEmpty @Size(max = 50)` on the list, and validate each element is not blank in a custom validator or via `@Valid`.

- **Verify all usages of `ProductAttributeDto`** in controllers and update accordingly:
  - `CreateProductRequestDto` — nested inside `ListingVariantCreationDto`
  - `UpdateListingRequest` in `ListingController`
  - Any admin update endpoints that accept attribute patches

### 2e — Mappers & Adapter

- **File:** `ProductHierarchyMapper.java` (or wherever attribute mapping lives)
  - Update `ProductAttribute` ↔ `ListingVariantAttributeEntity` mapping:
    - **Domain → Entity:** map `attr.values()` to a `List<ListingVariantAttributeValueEntity>`, each with `value` and `sortOrder` (index-based if not specified).
    - **Entity → Domain:** collect `entity.getValues()` into `List<String>` ordered by `sort_order`.

- **File:** `ProductHierarchyAdapter.java`
  - Confirm `saveVariant()` clears and rebuilds the attribute list (it already does this — verify it still works with the new child-value structure).

### 2f — Service Layer

- **File:** `CreateProductService.java`
  - Update the mapping from `VariantDefinition.attributes()` (list of `ProductAttribute`) — no logic change needed since `setAttributes()` is still called on the domain object. Verify the compiler is satisfied with the updated record shape.

---

## Phase 3: Tags System

**Goal: Replace the hardcoded `topSelling: boolean` and `featured: boolean` on `ListingVariant` with a flexible `ProductTag` entity that can be created and assigned without schema changes.**

### 3a — Schema Migration

- Create a new Flyway migration file.
- **Create `product_tags` table:**
  ```sql
  CREATE TABLE product_tags (
      id        BIGSERIAL PRIMARY KEY,
      name      VARCHAR(64) NOT NULL UNIQUE,
      color_hex CHAR(6) NOT NULL DEFAULT 'CCCCCC'
  );
  ```
- **Create `listing_variant_tags` junction table:**
  ```sql
  CREATE TABLE listing_variant_tags (
      variant_id BIGINT NOT NULL REFERENCES listing_variants(id) ON DELETE CASCADE,
      tag_id     BIGINT NOT NULL REFERENCES product_tags(id) ON DELETE CASCADE,
      PRIMARY KEY (variant_id, tag_id)
  );
  ```
- **Alter `listing_variants`:** Drop columns `top_selling` and `featured`.

### 3b — Domain Model

- **File:** Create `tj/radolfa/domain/model/ProductTag.java`
  - `record ProductTag(Long id, String name, String colorHex)`

- **File:** `ListingVariant.java`
  - Remove fields `topSelling: boolean` and `featured: boolean`.
  - Remove domain methods `updateTopSelling(boolean)` and `updateFeatured(boolean)`.
  - Add `tagIds: List<Long>` — the domain holds tag IDs only (avoids loading the full tag aggregate when mutating a variant).
  - Add domain method: `void assignTags(List<Long> tagIds)` — replaces the current tag set entirely (set-replace semantics, same pattern as `setAttributes`).

### 3c — JPA Entities

- **File:** Create `ProductTagEntity.java`
  - Table: `product_tags`
  - Fields: `id: Long`, `name: String` (unique), `colorHex: String`

- **File:** `ListingVariantEntity.java`
  - Remove `topSelling: boolean` and `featured: boolean`.
  - Add `@ManyToMany(fetch = FetchType.LAZY)` `@JoinTable(name = "listing_variant_tags", ...)` `private Set<ProductTagEntity> tags`.

### 3d — Ports

- **File:** Create `LoadProductTagPort.java`
  - `List<ProductTag> findAll()`
  - `Optional<ProductTag> findById(Long id)`
  - `List<ProductTag> findAllByIds(List<Long> ids)`

- **File:** Create `SaveProductTagPort.java`
  - `ProductTag save(String name, String colorHex)`

### 3e — Use Cases & Services

- **File:** Create `CreateProductTagUseCase.java` (in-port)
  - `Long execute(String name, String colorHex)`

- **File:** Create `AssignVariantTagsUseCase.java` (in-port)
  - `void execute(Long listingVariantId, List<Long> tagIds)` — replaces all current tags on the variant with the provided list (empty list = remove all tags).

- **File:** Create `CreateProductTagService.java` — validates name uniqueness, saves tag.

- **File:** Create `AssignVariantTagsService.java` — loads variant, validates all tagIds exist, calls `assignTags()`, saves variant.

### 3f — Persistence Adapter

- **File:** Create `ProductTagAdapter.java`
  - Implements `LoadProductTagPort` and `SaveProductTagPort`.
  - Add `ProductTagRepository extends JpaRepository<ProductTagEntity, Long>` with `findByName`.

- **File:** `ProductHierarchyAdapter.java`
  - Update `saveVariant()` to sync tags: resolve `List<Long> tagIds` → `Set<ProductTagEntity>` and set on entity.
  - Update the domain → entity mapping to populate `tagIds` from `entity.getTags()`.

### 3g — Controllers

- **File:** Create `ProductTagController.java`
  - `GET /api/v1/tags` — list all tags (public).
  - `POST /api/v1/admin/tags` — create a tag. Secured `ADMIN` only.
    - Request: `{ "name": "New Arrival", "colorHex": "DEF1DD" }`
    - Response: `201 Created` with `{ "id": 1, "name": "New Arrival", "colorHex": "DEF1DD" }`
  - `PUT /api/v1/admin/variants/{variantId}/tags` — assign tags to a variant. Secured `MANAGER` or `ADMIN`.
    - Request: `{ "tagIds": [1, 3] }` — replaces all existing tags.

### 3h — Read Model

- Update whatever DTO/read model is used for listing variant responses (e.g., the storefront listing response) to replace `topSelling: boolean`, `featured: boolean` with `tags: List<TagView>` where `TagView` is `{ id, name, colorHex }`.
- Update `ListingVariantDto` (or equivalent) in `application/readmodel` accordingly.

---

## Phase 4: Dimensions to Variant Level

**Goal: Move the four logistics fields (`weightKg`, `widthCm`, `heightCm`, `depthCm`) from `Sku` / `skus` table up to `ListingVariant` / `listing_variants` table. One logistics profile per color variant.**

### 4a — Schema Migration

- Create a new Flyway migration file.
- **Alter `listing_variants`:** Add columns:
  ```sql
  ALTER TABLE listing_variants
      ADD COLUMN weight_kg  DOUBLE PRECISION,
      ADD COLUMN width_cm   INT,
      ADD COLUMN height_cm  INT,
      ADD COLUMN depth_cm   INT;
  ```
- **Alter `skus`:** Drop the four dimension columns:
  ```sql
  ALTER TABLE skus
      DROP COLUMN weight_kg,
      DROP COLUMN width_cm,
      DROP COLUMN height_cm,
      DROP COLUMN depth_cm;
  ```

### 4b — Domain Models

- **File:** `Sku.java`
  - Remove fields: `weightKg`, `widthCm`, `heightCm`, `depthCm`.
  - Remove them from the constructor. Update any static factory or builder if present.
  - Update `updatePriceAndStock()` — no change needed, it only touches price and stock.

- **File:** `ListingVariant.java`
  - Add fields: `weightKg: Double`, `widthCm: Integer`, `heightCm: Integer`, `depthCm: Integer` (all nullable).
  - Add domain method: `void updateDimensions(Double weightKg, Integer widthCm, Integer heightCm, Integer depthCm)` — sets all four fields at once.

### 4c — JPA Entities

- **File:** `SkuEntity.java`
  - Remove the four dimension fields and their `@Column` annotations.

- **File:** `ListingVariantEntity.java`
  - Add the four dimension fields with nullable `@Column` annotations.

### 4d — DTOs & Request Models

- **File:** `SkuDefinitionDto.java` (nested inside `CreateProductRequestDto`)
  - Remove `weightKg`, `widthCm`, `heightCm`, `depthCm`.

- **File:** `ListingVariantCreationDto.java` (nested inside `CreateProductRequestDto`)
  - Add `Double weightKg`, `Integer widthCm`, `Integer heightCm`, `Integer depthCm` (all optional/nullable).

### 4e — Use Case Commands

- **File:** `CreateProductUseCase.java`
  - In `SkuDefinition` record: remove the four dimension fields.
  - In `VariantDefinition` record: add `Double weightKg`, `Integer widthCm`, `Integer heightCm`, `Integer depthCm`.

### 4f — Service Layer

- **File:** `CreateProductService.java`
  - When constructing the `ListingVariant` for each variant definition, call `updateDimensions(...)` using the values from `VariantDefinition`.
  - When constructing `SkuDefinition` objects: remove the dimension fields from the SKU creation path.

- **File:** Controller mapping in `ProductManagementController.java`
  - Update the mapping from `ListingVariantCreationDto` → `VariantDefinition` to pass the dimension fields at the variant level.
  - Update the mapping from `SkuDefinitionDto` → `SkuDefinition` to no longer include dimension fields.

### 4g — Mappers & Adapter

- **File:** `ProductHierarchyMapper.java` (or adapter mapping methods)
  - Update `Sku` ↔ `SkuEntity` mapping: remove the four dimension fields.
  - Update `ListingVariant` ↔ `ListingVariantEntity` mapping: add the four dimension fields.
  - Verify `saveVariant()` in the adapter sets dimensions on the entity from the domain object.

### 4h — Update Endpoint (if exists)

- If there is a PUT/PATCH endpoint for updating variant metadata (e.g., `PUT /api/v1/admin/variants/{id}`), update its request DTO to accept the dimension fields at the variant level and remove them from any SKU-level update DTO.

---

## Phase 5: Blueprint Validation + Final Cleanup

**Goal: Wire blueprint type validation into the product creation flow, and clean up any dead code or inconsistencies introduced across the previous phases.**

### 5a — Blueprint Validation in CreateProductService

- **File:** `CreateProductService.java`
  - The existing required-key validation (`validateRequiredAttributes`) only checks that required blueprint keys are present. Extend it to also validate:
    - For `ENUM` type blueprints: each value in `attribute.values()` must be one of the `allowedValues` in the blueprint.
    - For `MULTI` type blueprints: same — all submitted values must exist in `allowedValues`; the list may contain multiple valid values.
    - For `NUMBER` type blueprints: verify that `values` has exactly one element and it is parseable as a number.
    - For `TEXT` type blueprints: no constraint beyond presence (already covered by required check).
  - Throw a domain exception (e.g., `InvalidAttributeValueException`) with a message identifying the offending key and value when validation fails.
  - Add `InvalidAttributeValueException.java` to the domain exceptions package if it does not already exist.

### 5b — OpenAPI Annotations

- Add `@Operation` and `@ApiResponse` annotations to every new endpoint introduced in Phases 1–4:
  - `POST /api/v1/admin/categories/{categoryId}/blueprint`
  - `DELETE /api/v1/admin/categories/{categoryId}/blueprint/{blueprintId}`
  - `GET /api/v1/tags`
  - `POST /api/v1/admin/tags`
  - `PUT /api/v1/admin/variants/{variantId}/tags`
  - Any variant dimensions update endpoint added in Phase 4h

### 5c — Dead Code Removal

- Remove any references to `topSelling` or `featured` that survived Phase 3 (search the entire codebase).
- Remove any old single-value attribute references (`attrValue`) if the old column or field name appears anywhere.
- Confirm no orphaned migration files reference the dropped columns.

### 5d — Smoke Test

- Build with `./mvnw clean compile` — must pass with zero errors.
- Manually verify via the API (Swagger or curl):
  1. Create a category, then `POST /api/v1/admin/categories/{id}/blueprint` with entries of each type (TEXT, NUMBER, ENUM, MULTI).
  2. Create a product with 2 variants — each with multi-value attributes (`values: ["Cotton", "Polyester"]`), dimensions at the variant level, and no topSelling/featured fields.
  3. Create a tag via `POST /api/v1/admin/tags` and assign it to a variant via `PUT /api/v1/admin/variants/{id}/tags`.
  4. Confirm the product read response has `tags: [...]`, `dimensions` at the variant level, and `values: [...]` on each attribute.

---

## Schema & Persistence Instructions for Claude Code

**This is an early development phase with no production data and no risk of data loss.** Do not pause or ask for permission regarding schema changes. If you need to drop/recreate tables, alter columns, or replace Flyway migrations — do it. Treat all schema operations as zero-risk.

Every schema change in this plan must be implemented as a new Flyway migration file (`V{N}__description.sql`). Do not edit existing migration files. Use the next available version number by checking the existing migrations in `src/main/resources/db/migration/`.
