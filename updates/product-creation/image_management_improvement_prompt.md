# Prompt: Redesign Image Management UI in Product Creation

## Objective
The current image management in the product creation wizard is "boring" and the "Main Image" icon is "ugly." Also, the user cannot easily switch which image is the main one. I want to redesign this.

## Relevant Files
- **Component File**: `frontend/src/features/product-creation/ui/steps/Step2Variants.tsx`
- **Internal Component**: `MediaZone` (lines 447-681)
- **State File**: `frontend/src/features/product-creation/model/types.ts`
- **API File**: `frontend/src/features/product-creation/api/imageUpload.ts`

## Tasks
1. **Redesign the `MediaZone` UI**:
   - The current "Focal image layout" (a large main image and a small grid is not good, everything should be the same size) needs to be more premium. Use better shadows, rounded corners, and consistent padding.
   - Design a better "Main" indicator icon. Instead of a simple banner, use a more professional overlay or integrated icon (e.g., a "Star" or "Main" badge in the corner).

2. **Implement Drag-and-Drop Reordering**:
   - Enable dragging secondary images onto the main image position to make them the "Main" image.
   - Optimally, allow reordering the entire grid using a library like `dnd-kit` or `react-beautiful-dnd` (or clean native drag-and-drop).
   - Ensure `images[0]` always represents the main image in the `VariantDraft.images` array.

3. **Improve the "Add Image" Zone**:
   - Make the "Drop photos here or click to browse" area look more modern. Use subtle animations on hover/drag-over.

4. **Styling**:
   - Ensure the UI looks clean, with clear feedback during upload (`Loader2`) and errors (`AlertCircle`).

## Constraints
- Stay within the `Step2Variants.tsx` file or create a dedicated subdirectory `frontend/src/features/product-creation/ui/components/media` if extracting components.
- Do not break the `onUploaded` and `onDelete` logic that interacts with the `WizardState`.
