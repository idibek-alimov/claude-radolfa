# Radolfa Frontend: Product Creation Wizard — Implementation Plan

> **Development Phase Notice:** This plan is executed during an **early development phase**. Deleting and rebuilding components, moving files, and changing state shapes is explicitly permitted. Do not treat any refactor as risky.

## Goal (Inspired by Wildberries)
Build a full-page, 5-step wizard at `app/(admin)/manage/products/create/page.tsx` that allows managers to create a multi-variant product in one guided flow. The wizard must be **retired as a replacement of the existing `CreateProductDialog`** — delete `features/product-creation/ui/CreateProductDialog.tsx` and update callers to navigate to the new route.

**Stack:** Next.js 15 · React 19 · TanStack Query · Tailwind CSS · Radix UI · Framer Motion

---

## Execution Tracker

> **Instructions for Claude Code:** Execute this plan ONE step at a time. After completing a step, update this table to change the status to `✅ COMPLETE` and add a concise 1-sentence summary. **Do not proceed to the next step until the user reviews the current one.**

| Step | Status | Summary of Implementation |
|---|---|---|
| **Step 1:** State Model & Scaffold | ✅ COMPLETE | Created `WizardState` types, `useWizardState` hook with localStorage persistence, `fetchBlueprint` API call, `WizardStepper`, `WizardFooter`, `ProductCreationWizard` orchestrator with Framer Motion slide transitions, and the page route at `app/(admin)/manage/products/create/page.tsx`; "New Product" button on manage page now navigates to the new route instead of opening the old dialog. |
| **Step 2:** Step 1 — Classification & Setup | ⏳ PENDING | |
| **Step 3:** Step 2 — Media Upload | ⏳ PENDING | |
| **Step 4:** Step 3 — Variant Matrix | ⏳ PENDING | |
| **Step 5:** Step 4 — Dynamic Attributes | ⏳ PENDING | |
| **Step 6:** Step 5 — Review & Submit | ⏳ PENDING | |
| **Step 7:** Wizard Shell & Stepper | ⏳ PENDING | |
| **Step 8:** Final Cleanup | ⏳ PENDING | |

---

## Backend APIs Used

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/v1/categories` | Load category tree for Step 1 |
| `GET` | `/api/v1/categories/{id}/blueprint` | Load dynamic attributes after category selected |
| `GET` | `/api/v1/colors` | Load colors for Step 1 multi-select |
| `GET` | `/api/v1/brands` | Load brands for Step 1 (⚠️ `BrandController` may not exist — if missing, omit brand field entirely rather than blocking) |
| `POST` | `/api/v1/admin/images/upload` | Upload image; returns `{ url: string }` |
| `POST` | `/api/v1/admin/products` | Submit final creation payload |

---

## Wizard State Shape

The wizard holds a single `WizardState` object throughout all steps. Use this exact shape:

```typescript
interface WizardState {
  // Step 1
  name: string;
  categoryId: number | null;
  brandId: number | null;         // optional
  colorIds: number[];             // min 1 required
  webDescription: string;         // shared across all variants, duplicated on send
  attributes: { key: string; value: string; sortOrder: number }[]; // shared, duplicated on send

  // Step 2 (per-color images)
  imagesByColorId: Record<number, string[]>;  // colorId → array of S3 URLs

  // Step 3 (per-color + size SKU rows)
  skuRows: {
    _key: string;           // crypto.randomUUID() for React keying
    colorId: number;
    sizeLabel: string;      // @NotBlank backend constraint — must not be empty
    price: number;
    stockQuantity: number;
    barcode: string;        // @NotBlank backend constraint — must not be empty
    weightKg?: number;
    widthCm?: number;
    heightCm?: number;
    depthCm?: number;
  }[];
}
```

Place state model and types in `features/product-creation/model/wizardState.ts`.

---

## Step 1 (Implementation): State Model & Scaffold

**Goal:** Set up the wizard skeleton, routing, and state management before any real UI.

- Create `app/(admin)/manage/products/create/page.tsx`. This is the wizard host.
- Create `features/product-creation/ui/ProductCreationWizard.tsx` — the main component rendered by the page.
- Create `features/product-creation/model/wizardState.ts` with the `WizardState` interface and a `defaultWizardState` constant.
- Implement draft persistence: on every state update, save wizard state to `localStorage` using key `radolfa:product-creation-draft`. On mount, restore from `localStorage` if present. Clear on successful submission.
- Add step-navigation logic: `currentStep` (1–5), `goNext()`, `goPrev()`, `goToStep(n)`.
- Render placeholder `<div>Step {currentStep}</div>` for each step. The stepper header will be wired in the last step.

---

## Step 2 (Implementation): Classification & Setup (Wizard Step 1)

**Goal:** Name, category, brand, colors, and shared description.

**Component:** `features/product-creation/ui/steps/ClassificationStep.tsx`

- **Product Name** — required text `<Input>`. Validate: not blank.
- **Category** — required. Flat searchable `<select>` built from `GET /api/v1/categories` (flatten tree with indent). On change, fire a background `useQuery` for `GET /api/v1/categories/{id}/blueprint` to pre-warm Step 5.
- **Brand** — optional `<select>` from `GET /api/v1/brands`. If the endpoint returns 404 or doesn't exist, hide the Brand field entirely (don't show an error to the user).
- **Colors** — multi-select from `GET /api/v1/colors`. Render as colored chips (use `hexCode` for the swatch dot; `hexCode` is `string | null` — fall back to `#e5e7eb` when null). Min 1 required.
  - **Color deselection guard:** If a color being deselected already has entries in `imagesByColorId[colorId]`, show a confirmation: "Removing this color will discard N uploaded images. Continue?" On confirm, delete `imagesByColorId[colorId]` from state.
- **Product Description (shared)** — `<textarea>` for `webDescription`. Will be sent to all variants.
- Validation: name required, categoryId required, at least 1 color required.

---

## Step 3 (Implementation): Media Upload (Wizard Step 2)

**Goal:** Upload images grouped by color.

**Component:** `features/product-creation/ui/steps/MediaStep.tsx`

- Render one upload zone **per selected color** (use tabs or side-by-side columns).
- Each zone accepts **drag-and-drop** and **click-to-browse** file inputs.
- On file drop or selection, call `POST /api/v1/admin/images/upload` immediately.
  - **Critical:** Use `formData.append('image', file)` — the backend expects the field name `image` (singular).
  - Show a spinner/skeleton while uploading.
  - On success (returns `{ url }`), push the URL into `imagesByColorId[colorId]` in state and show the image thumbnail.
  - On failure, show an inline error next to that image slot.
- Allow deletion: remove a URL from `imagesByColorId[colorId]`. No API call needed (S3 file stays, not referenced).
- Images field is optional in the backend DTO — no minimum validation needed.

---

## Step 4 (Implementation): Variant Matrix (Wizard Step 3)

**Goal:** Define sizes and logistics per color variant.

**Component:** `features/product-creation/ui/steps/VariantMatrixStep.tsx`

**Table initialization:**
- On entering this step, ensure there is at least one `skuRow` per selected color. If a color has no rows yet, auto-generate one with `sizeLabel: "ONE_SIZE"`.
- User can add more rows per color with `+ Add Size` button.
- User can remove a row (keep at least 1 per color; disable delete if only 1 row for that color).

**Table columns (flat — every row is one Color + Size combination):**

| Column | Type | Required? |
|---|---|---|
| Color | read-only chip (use `hexCode ?? '#e5e7eb'` for swatch) | — |
| Size Label | text input | ✅ `@NotBlank` |
| Barcode | text input | ✅ `@NotBlank` |
| Price (TJS) | number input (≥ 0) | ✅ |
| Stock | integer input (≥ 0) | ✅ |
| Weight (kg) | number input | Optional |
| Width × Height × Depth (cm) | 3 number inputs | Optional |
| Delete | icon button | — |

**"Apply to All" feature:**
- Price and Stock column headers each have an `⬇ Apply to color` action.
- Clicking it takes the first row's value for that column **within the same color group** and fills it down to all other rows of the same color.
- Optionally add a secondary "Apply to all variants" that fills across all color groups.

**Validation (blocking):** Before allowing "Next", ensure no row has an empty `sizeLabel` or empty `barcode`. Show inline red errors on offending cells.

---

## Step 5 (Implementation): Dynamic Attributes (Wizard Step 4)

**Goal:** Fill category-specific characteristics driven by the blueprint.

**Component:** `features/product-creation/ui/steps/AttributesStep.tsx`

- Use the blueprint data pre-fetched in Step 2 (classification). Read from `useQuery(['blueprint', categoryId])`.
- For each `BlueprintEntryDto { attributeKey, required, sortOrder }`, render a labeled text `<Input>`.
  - Required entries marked with red asterisk `*`.
  - Render in `sortOrder` order.
- If the blueprint is empty (no entries), show: "No required attributes for this category" and render a `+ Add Attribute` button to let the user add free-form key/value pairs manually.
- All values are stored in the shared `attributes[]` array in `WizardState`. On final submission, the same array is sent to every variant.
- Validation: all `required: true` blueprint entries must have a non-empty value before "Next".

---

## Step 6 (Implementation): Review & Submit (Wizard Step 5)

**Goal:** Final summary and API call.

**Component:** `features/product-creation/ui/steps/ReviewStep.tsx`

**Summary display:**
- Product name, category (name), optional brand, number of colors, total SKU count.
- Per-color: number of images, list of sizes.

**Blocking validation before submit:**
- Disable "Create Product" button if any `skuRow` has an empty `sizeLabel` or empty `barcode`. These cause a `400 Bad Request` — treat as hard errors not warnings.
- Show specific error messages: "Color {name} has X SKUs with no barcode."

**Payload transformation:**
```typescript
const payload = {
  name: state.name,
  categoryId: state.categoryId,
  brandId: state.brandId ?? undefined,
  variants: state.colorIds.map(colorId => ({
    colorId,
    webDescription: state.webDescription || undefined,
    attributes: state.attributes,       // same for all colors
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

**On success:**
- Backend returns `{ productBaseId: number }` — do NOT use this as a slug.
- Show `sonner` success toast.
- Clear `localStorage` key `radolfa:product-creation-draft`.
- Redirect to `/manage` (the product management list page).

**On error:**
- Show field-level backend validation messages if available (Spring returns them in 400 body).
- Fall back to a generic error toast.

---

## Step 7 (Implementation): Wizard Shell, Stepper & Animations

**Goal:** Wire all step components into the `ProductCreationWizard` with a polished shell.

- **Stepper header:**  Horizontal stepper at the top, showing steps 1–5 with:
  - Completed: ✓ icon, filled color
  - Current: highlighted border/ring
  - Pending: muted / greyed out
  - Clicking a completed step allows navigating back.
- **Sticky footer:** Previous / Next (or "Create Product" on Step 5) buttons fixed to the bottom of the viewport.
- **Framer Motion transitions:** Wrap step content in `<AnimatePresence>`. Use a slide-left / slide-right animation based on navigation direction.
- **Page layout:** Full-width page with a max-width content container. Use the project's existing admin layout.

---

## Step 8 (Implementation): Final Cleanup

- Delete `features/product-creation/ui/CreateProductDialog.tsx` (replaced by the full-page wizard).
- Find all callers of `CreateProductDialog` and replace with a navigation link/button to `/manage/products/create` (`(admin)` is a layout group, not a URL segment).
- Confirm all new API calls use the existing `axios` instance from `@/shared/api` (same base URL and auth interceptors as the rest of the app).
- Test the full happy path: create a 2-color × 3-size product with images and attributes.
- Ensure the wizard recovers a draft from `localStorage` on page refresh mid-flow.
