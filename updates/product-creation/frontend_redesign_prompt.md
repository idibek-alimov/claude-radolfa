# Radolfa Product Creation Redesign: Wildberries Implementation Prompt

> **Development Phase Notice:** This plan is executed during an **early development phase**. Deleting and rebuilding components, moving files, and changing state shapes is explicitly permitted. Do not treat any refactor as risky. This replaces the existing 5-step wizard with a streamlined 3-step WB-inspired wizard.

## Goal & Architecture Changes

We are redesigning the frontend product creation wizard to match Wildberries seller experience. The 5 complaints being fixed:
1. **Width/Alignment:** Expand the container from `max-w-3xl` to `max-w-7xl` / `px-8` to align with the navbar.
2. **Category Blueprints First:** Attributes appear immediately on category selection in Step 1, not isolated in Step 4.
3. **Color Selection UX:** Replace chip multi-select with an explicit "Add Color Variant" button that creates a tab.
4. **Variant Matrix Stacking:** Manage variants in Tabs (one per color), combining Media and SKU matrix inside each tab.
5. **Dual Visibility Flags:** Two separate flags are added per variant:
   - `isPublished` — set **once at creation** by the manager as a signal that "this variant is complete and intentional". Defaults `false` (draft). Variant is never visible to customers until published.
   - `isActive` — **runtime on/off toggle** that ops flip at any point to show/hide a published variant without removing it. Defaults `true`. Useful for seasonal pauses, stock issues, etc.
   - Storefront rule: visible to customers only when **both `isPublished = true` AND `isActive = true`**.

---

## Execution Tracker

> **Instructions for Claude Code:** Execute ONE step at a time. After completing a step, update the table status to `✅ COMPLETE` and add a brief summary. **Do not proceed to the next step until the user reviews.**

| Step | Status | Summary |
|---|---|---|
| **Step 1:** Add `isPublished` + `isActive` to Backend | ✅ COMPLETE | Added 2 SQL columns, JPA entity fields, domain constructor params (13 total), getters & mutation methods, DTO fields, UseCase `VariantDefinition` fields, mapper + service wiring, controller null-safe defaults. All 117 tests pass. |
| **Step 2:** Refactor `WizardState` | ✅ COMPLETE | Replaced `colorIds[]` + `imagesByColorId{}` + `skuRows[]` with `variants: VariantDraft[]`. Removed `colorId` from `SkuRow`. Added `VariantDraft` interface with `isPublished`/`isActive` flags. Updated `validateStep1` (no more colorIds check), renamed SKU validation to `validateStep2`/`Step2Errors`, kept backward-compat aliases so old step files keep compiling. Updated `buildPayload` in `createProduct.ts`. Zero TS errors in edited files. |
| **Step 3:** Step 1 — Base Info & Characteristics | ⏳ PENDING | |
| **Step 4:** Step 2 — Variants & Media (Tabs) | ⏳ PENDING | |
| **Step 5:** Step 3 — Review & Submit | ⏳ PENDING | |
| **Step 6:** Shell Expansion & Cleanup | ⏳ PENDING | |

---

## Step 1: Add `isPublished` and `isActive` to Backend

**Goal:** Introduce two lifecycle/visibility flags to the DB, domain, and DTOs.

**Early dev note — no new Flyway migration.** Edit the baseline directly:
- Open `V1__baseline_clean_schema.sql`. Inside `CREATE TABLE listing_variants (...)`, add these two columns **after** the `featured` column:
  ```sql
  is_published    BOOLEAN  NOT NULL DEFAULT FALSE,
  is_active       BOOLEAN  NOT NULL DEFAULT TRUE,
  ```
- Add to `ListingVariantEntity`:
  ```java
  @Column(name = "is_published", nullable = false)
  private boolean isPublished = false;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;
  ```
- Add both fields to the `ListingVariant` domain class: new constructor params, getters, and mutation methods `updateIsPublished(boolean)` and `updateIsActive(boolean)`.
- Add `Boolean isPublished` and `Boolean isActive` to `CreateProductRequestDto.ListingVariantCreationDto`. Thread both into `CreateProductUseCase.Command.VariantDefinition`.
- Update all `ListingVariant` constructor call-sites in `ProductHierarchyMapper`, `CreateProductService`, and affected test files.

---

## Step 2: Refactor `WizardState`

**Goal:** Restructure local state so images and SKUs live inside their variant objects.
- **File:** `features/product-creation/model/types.ts`
- Remove `colorIds`, `imagesByColorId`, and `skuRows` from the root `WizardState`.
- Add a `variants` array:
  ```typescript
  variants: {
      colorId: number;
      isPublished: boolean; // default false
      isActive: boolean;    // default true
      images: string[];
      skus: SkuRow[];
  }[];
  ```
- Update all validation functions to operate on the new `variants` array.

---

## Step 3: Step 1 — Base Info & Characteristics

**Goal:** Merge old Step 1 (Classification) and Step 4 (Attributes) into one step.
- Create `Step1BaseInfo.tsx` (delete `Step1Classification.tsx` and `Step4Attributes.tsx`).
- Fields: Product Name, Category (dropdown), Brand (optional), Shared Web Description.
- On category select → immediately `fetchBlueprint` and render the blueprint attribute fields inline (like Wildberries "Base Information" card). Required blueprint fields must be filled before Next is enabled.
- No color selection here — color is handled in Step 2.

---

## Step 4: Step 2 — Variants & Media (Tabs)

**Goal:** One unified step for all per-color configuration.
- Create `Step2Variants.tsx` (delete `Step2Media.tsx` and `Step3VariantMatrix.tsx`).
- "Add Color Variant" button opens a popover/dialog to pick a color. Selecting one pushes a new object into `state.variants` and creates a new Tab.
- Each Tab contains:
  1. **"Publish Variant"** toggle bound to `variant.isPublished`. When off, variant is saved as a draft and never shown to customers.
  2. **"Show to Customers"** toggle bound to `variant.isActive`. Only meaningful when `isPublished = true`; lets ops temporarily hide a live variant.
  3. **Media Zone** — drag-and-drop upload zone and thumbnail list for `variant.images`.
  4. **SKU Matrix** — table with Size, Barcode, Price, Stock columns for `variant.skus`. "Apply down" bulk-fill is scoped to this tab only.
- Validation blocks advancing to Step 3 if any SKU row in any tab is missing a size label or barcode.

---

## Step 5: Step 3 — Review & Submit

**Goal:** Update review screen for new state shape and add status badges.
- Rename/repurpose `Step5Review.tsx` → `Step3Review.tsx` and update all imports.
- Show per-variant status badge:
  - 🔴 **Draft** — `isPublished = false`
  - 🟡 **Hidden** — `isPublished = true`, `isActive = false`
  - 🟢 **Live** — `isPublished = true`, `isActive = true`
- Update `api/createProduct.ts` `buildPayload` to map from `state.variants` (including `isPublished` and `isActive`) into the `CreateProductRequestDto`.

---

## Step 6: Shell Expansion & Cleanup

**Goal:** Remove width constraints and reduce wizard to 3 steps.
- In `ProductCreationWizard.tsx`:
  - Remove `max-w-3xl` from the stepper header and main content. Use `max-w-7xl` or `w-full px-8`.
  - Set `TOTAL_STEPS = 3` and update step labels to: `["Base Info", "Variants & Media", "Review"]`.
- Delete dead files: `Step1Classification.tsx`, `Step2Media.tsx`, `Step3VariantMatrix.tsx`, `Step4Attributes.tsx`.
