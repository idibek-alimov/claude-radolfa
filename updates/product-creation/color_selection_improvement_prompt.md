# Prompt: Improve Color Selection UI in Product Creation

## Objective
Redesign the color selection popup in the product creation wizard to make it feel more premium and visually appealing. The current design is "boring" and the background is too dark.

## Relevant Files
- **Component File**: `frontend/src/features/product-creation/ui/steps/Step2Variants.tsx`
- **Internal Component**: `ColorPickerDialog` (lines 364-416)
- **State File**: `frontend/src/features/product-creation/model/types.ts` (defines `WizardState` and `VariantDraft`)

## Tasks
1. **Redesign the `ColorPickerDialog`**:
   - Make the dialog background lighter and more sophisticated (e.g., using a subtle glassmorphism effect or a very light gray instead of the default dark overlay).
   - Improve the color button design in the grid. Instead of a simple border, use cards with subtle shadows, better typography, and perhaps a larger color circle.
   - Add hover effects that make the selection feel interactive.
   - Ensure the "All available colors have been added" empty state also looks premium.

2. **Styling**:
   - Use Vanilla CSS or tailwind (following the project's lead in `@/shared/lib/utils` and `cn`).
   - Focus on modern, clean aesthetics (Inter/Roboto font, smooth rounded corners, consistent spacing).

## Constraints
- Do not change the underlying logic of `onSelect` or `usedColorIds`.
- Stay within the `Step2Variants.tsx` file for the component redesign, or extract it to a new file in `frontend/src/features/product-creation/ui/components` if it improves maintainability.
