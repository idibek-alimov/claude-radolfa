# Phase 1: Product Create & Edit Overhaul - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Replace the product creation popup with a full-page form at `/manage/products/new`. Fix the edit page layout width and replace the plain textarea description field with a rich text editor. Backend adds `webDescription` to the create endpoint.

</domain>

<decisions>
## Implementation Decisions

### Rich text editor
- **D-01:** Use TipTap (headless, ProseMirror-based). Install `@tiptap/react`, `@tiptap/starter-kit`.
- **D-02:** Editor used in two places: the new create page and the existing `EnrichmentCard` on the edit page.
- **D-03:** Keep the 5000-character limit already enforced by `EnrichmentCard`. Show char count below editor on both pages.

### Create page layout
- **D-04:** Two-column layout — left column: name, category, color, rich text description. Right column: image upload zone + SKU table.
- **D-05:** Page uses `max-w-[1600px]` container (same as the admin dashboard target width from EDIT-01).
- **D-06:** FSD placement: new feature slice `features/product-creation-page/` with a `CreateProductPage` component. The `app/(admin)/manage/products/new/page.tsx` route imports it. The old `CreateProductDialog` remains but the "New Product" button on the manage page navigates to the new route instead of opening the dialog.

### Image upload on create page
- **D-07:** Simple file input (click to browse, no drag-drop required). Multiple files accepted. Images are optional (CREATE-04).
- **D-08:** Upload happens after product creation succeeds (CREATE-03 sequence: create product → upload images if any → redirect to edit page).
- **D-09:** No elaborate progress indicator needed — a loading spinner on the submit button is sufficient. Button label changes: "Creating…" during product creation, "Uploading images…" during image upload.

### Edit page width fix
- **D-10:** Change `max-w-3xl` to `max-w-[1600px]` in `ProductEditPage.tsx`. No other layout changes to the edit page.

### Claude's Discretion
- Exact TipTap toolbar buttons (bold, italic, bullet list, ordered list is a reasonable default set)
- SKU table reuse strategy on create page (can reuse `SkuTableCard` logic or inline it)
- Error handling for partial failures (image upload fails after product created — toast error, stay on create page or redirect anyway)

</decisions>

<specifics>
## Specific Ideas

- The new create page should feel consistent with the edit page cards — same `bg-card rounded-xl border shadow-sm p-6` card style.
- The manage page (`/manage`) currently opens a dialog on "New Product" click — replace that with a `router.push('/manage/products/new')` navigation.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Architecture & conventions
- `.planning/codebase/ARCHITECTURE.md` — hexagonal layer rules, FSD dependency direction
- `.planning/codebase/STACK.md` — tech stack and installed packages
- `frontend/CLAUDE.md` — FSD rules, image upload API (`POST /listings/{slug}/images`, field `files`), role model, query keys
- `backend/CLAUDE.md` — hexagonal guardrails, DTO as records, MapStruct mapping rules

### Key files to read before modifying
- `frontend/src/features/product-creation/ui/CreateProductDialog.tsx` — existing dialog to be replaced/kept
- `frontend/src/features/product-edit/ui/ProductEditPage.tsx` — width fix target (`max-w-3xl` → `max-w-[1600px]`)
- `frontend/src/features/product-edit/ui/EnrichmentCard.tsx` — textarea → TipTap replacement
- `frontend/src/app/(admin)/manage/page.tsx` — "New Product" button navigation change
- `frontend/src/entities/product/api/admin.ts` — `createProduct` API call (add `webDescription` param)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `EnrichmentCard.tsx`: Already has `webDescription` state + `updateListing` mutation — TipTap replaces the `<textarea>` only; all surrounding save logic stays.
- `CreateProductDialog.tsx`: SKU table + category/color selects logic can be ported directly to the new page.
- `shared/ui/` Shadcn components: `Button`, `Input`, `Card` pattern (`bg-card rounded-xl border shadow-sm p-6`) used across edit page cards.

### Established Patterns
- Image upload: `POST /listings/{slug}/images` with `multipart/form-data`, field name `files` — already used in `ImageCard.tsx`.
- Form validation: inline `errors` state record pattern from `CreateProductDialog` — replicate on new page.
- TanStack Query mutations with `toast.success` / `toast.error` via `getErrorMessage()` — standard pattern across all features.

### Integration Points
- Backend `CreateProductUseCase` / `CreateProductRequestDto` — add `webDescription: String` (nullable).
- `app/(admin)/manage/products/new/page.tsx` — new route file needed.
- `manage/page.tsx` — dialog open state replaced by router navigation.

</code_context>

<deferred>
## Deferred Ideas

- Drag-and-drop image upload — noted but not in scope for this phase
- Multi-color variant creation in one form — explicit out of scope (REQUIREMENTS.md)
- Draft/publish workflow — explicit out of scope

</deferred>

---

*Phase: 01-product-create-edit-overhaul*
*Context gathered: 2026-03-21*
