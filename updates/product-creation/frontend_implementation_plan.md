# Frontend Implementation Plan: Product Creation Wizard

**Stack:** Next.js 15 · React 19 · TanStack Query · Tailwind CSS · Radix UI · Framer Motion  
**Route:** `app/(admin)/manage/products/create/page.tsx` (full page — NOT a dialog)  
**API Base:** `/api/v1`

> All 4 architecture decisions are resolved:
> - Colors selected in Step 1 (so media step can tag images by color immediately)
> - Attributes are shared across all color variants (duplicated on send)
> - `webDescription` is shared (one textarea, duplicated on send)
> - Full-page dedicated route

---

## Wizard State Shape

The wizard maintains a single state object throughout all steps. This is the canonical shape Claude Code must use:

```typescript
interface WizardState {
  // Step 1
  name: string;
  categoryId: number | null;
  brandId: number | null;         // optional
  colorIds: number[];             // multi-select, min 1
  webDescription: string;         // shared, duplicated to all variants on submit
  attributes: { key: string; value: string; sortOrder: number }[]; // shared

  // Step 2 (per-color images)
  imagesByColorId: Record<number, string[]>;  // colorId -> array of uploaded S3 URLs

  // Step 3 (per-color+size SKU table)
  skuRows: {
    _key: string;         // uuid for list diffing
    colorId: number;
    sizeLabel: string;
    price: number;
    stockQuantity: number;
    barcode: string;      // @NotBlank — required
    weightKg?: number;
    widthCm?: number;
    heightCm?: number;
    depthCm?: number;
  }[];
}
```

---

## Backend APIs Used

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/v1/categories` | Load category tree for Step 1 |
| `GET` | `/api/v1/categories/{id}/blueprint` | Load dynamic attributes after category selected |
| `GET` | `/api/v1/colors` | Load colors for Step 1 multi-select |
| `GET` | `/api/v1/brands` | Load brands for Step 1 dropdown (Note: `BrandController` does not exist yet; either build it or omit brand fetching for now) |
| `POST` | `/api/v1/admin/images/upload` | Upload image file, returns `{ url }` |
| `POST` | `/api/v1/admin/products` | Submit final creation payload |

---

## Step 1: Classification & Setup

**Fields:**
- Product Name (`name`) — required text input
- Category (`categoryId`) — required searchable tree selector
- Brand (`brandId`) — optional dropdown
- Colors (`colorIds`) — multi-select with color swatches (hexCode from color entity), min 1
- Product Description (`webDescription`) — shared textarea for all variants
- *Blueprint loads automatically in background once category is selected*

**Behavior:**
- As soon as `categoryId` is set, fire `useQuery(['blueprint', categoryId], ...)` to prefetch the Step 4 attribute form.
- Colors are displayed as colored chips with `displayName`. User can select multiple.
- If a user deselects a color that already has uploaded images in `imagesByColorId`, warn them: "Removing this color will discard X uploaded images" and clear `imagesByColorId[colorId]` on confirmation.
- Validation: name required, categoryId required, at least 1 color required.

---

## Step 2: Media Upload

**Component:** `MediaUploader`

**Behavior:**
- The UI shows one upload zone **per selected color** (tabs or columns, one per color chip from Step 1).
- Each zone accepts drag-and-drop or click-to-browse file inputs.
- On file selection, immediately call `POST /api/v1/admin/images/upload` with the file. **Crucially, use `formData.append('image', file)` as the backend specifically expects the singular field name `image`.**
- Show a loading spinner per image while uploading. On success, display the image thumbnail and store the returned `url` in `imagesByColorId[colorId]`.
- Users can delete uploaded images (remove URL from state; file is already on S3 — no delete call needed).
- Images are **grouped by color** from the start — no reassignment needed.
- Validation: No minimum required (images are optional per the backend DTO).

---

## Step 3: Variant Matrix

**Component:** `VariantTable`

**Behavior:**
- On entering this step, auto-generate one default `skuRow` per selected color (from Step 1) with `sizeLabel` set to "ONE_SIZE" (to avoid forcing users to type a size for single-size products).
- The user can add more rows per color (e.g., S, M, L for each color).
- The table is flat — each row is `colorId + size + logistics`.
- Color column is read-only (derived from `colorIds`). All other columns are editable.

**Table Columns:**
| Column | Type | Notes |
|---|---|---|
| Color | display only | Color chip / name |
| Size Label | text | e.g. "S", "M", "42" |
| Barcode | text | **Required** — unique per SKU |
| Price (TJS) | number | Required, ≥ 0 |
| Stock | integer | Required, ≥ 0 |
| Weight (kg) | number | Optional |
| W × H × D (cm) | 3 number inputs | Optional |
| Delete | button | Remove row (if >1 row per color) |

**Power Feature — "Apply to All":**
- Price and Stock columns have an `⬇ Apply to all` button on the header.
- Clicking it copies the value from the first row of that column to all other rows **within the same color group** by default (with an optional "all colors" action if needed).

---

## Step 4: Characteristics (Attributes)

**Component:** `DynamicAttributeForm`

**Behavior:**
- Renders a form field for each `BlueprintEntryDto` returned by `GET /api/v1/categories/{id}/blueprint`.
- If the blueprint is empty (no blueprint configured for the category), show a message: "No required attributes for this category" and allow manual attribute addition with `+ Add Attribute` (key + value inputs).
- `required: true` blueprint entries are marked with a red asterisk `*`.
- All values are stored in the shared `attributes` array in wizard state.
- On submission, the same `attributes` array is duplicated to every variant.

---

## Step 5: Review & Submit

**Component:** `ReviewStep`

**Behavior:**
- Show a read-only summary: product name, category, brand, number of variants, number of SKUs.
- Ensure blocking validation: The "Create Product" button must be disabled if any SKU has an empty `sizeLabel` or an empty `barcode` (`@NotBlank` on backend). These are hard runtime errors, not soft warnings.
- "Create Product" button triggers payload transformation and the API call.

**Payload Transformation:**
```typescript
const payload = {
  name: state.name,
  categoryId: state.categoryId,
  brandId: state.brandId ?? undefined,
  variants: state.colorIds.map(colorId => ({
    colorId,
    webDescription: state.webDescription || undefined,
    attributes: state.attributes,  // same for all colors
    images: state.imagesByColorId[colorId] ?? [],
    skus: state.skuRows
      .filter(row => row.colorId === colorId)
      .map(({ sizeLabel, price, stockQuantity, barcode, weightKg, widthCm, heightCm, depthCm }) => ({
        sizeLabel, price, stockQuantity, barcode,
        weightKg: weightKg ?? undefined,
        widthCm: widthCm ?? undefined,
        heightCm: heightCm ?? undefined,
        depthCm: depthCm ?? undefined,
      }))
  }))
};
```

- On success: The backend returns `{ productBaseId }`. Show a `sonner` toast, and redirect to the product management list page. Do not attempt to redirect to the new product's edit page using a slug, as the response does not contain the slug.
- On error: show field-level backend error messages if available, or a generic toast.

---

## UI / UX Requirements

- **Stepper header:** Horizontal stepper at the top of the page showing Step 1–5 with status (complete ✓, current, pending).
- **Sticky footer:** Previous / Next buttons stick to the bottom of the viewport as the user scrolls.
- **Draft persistence:** Save wizard state to `localStorage` on every step change using a specific key (e.g., `radolfa:product-creation-draft`) to avoid collisions. Restore on page load. Clear on successful submission.
- **Transitions:** Subtle Framer Motion slide animation between steps (slide left/right based on direction).
- **Validation:** Each step validates its own fields before allowing "Next". Show errors inline.

---

## FSD Layer Placement

- **Page:** `app/(admin)/manage/products/create/page.tsx`
- **Wizard orchestrator:** `features/product-creation/ui/ProductCreationWizard.tsx`
- **Step components:** `features/product-creation/ui/steps/` (one file per step)
- **API calls:** `features/product-creation/api/` (upload, create)
- **State/types:** `features/product-creation/model/`
- **Retire old code:** Delete `features/product-creation/ui/CreateProductDialog.tsx` as this full-page wizard replaces it. Update any files calling it to link to the new route instead.
