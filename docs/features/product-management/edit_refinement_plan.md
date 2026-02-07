# Refinement Plan: Product Edit Page & Image Management

This plan details the necessary refinements to the product edit experience, ensuring strict adherence to ERP data ownership and providing robust image management for Managers.

## Proposed Changes

### 1. Frontend: API Layer Enhancement
- **File**: `frontend/src/features/products/api.ts`
- **New Function**: Add `uploadProductImage(erpId: string, file: File)`:
  - Uses `FormData` to wrap the file.
  - Calls `POST /api/v1/products/${erpId}/images`.
  - Returns `Promise<ProductImageResponse>`.

### 2. Frontend: Edit Dialog Refinement
- **File**: `frontend/src/app/(admin)/manage/page.tsx`
- **Field Constraints**:
  - Update `name`, `price`, and `stock` inputs to be `readOnly` or `disabled`.
  - Visual: Maintain current "Lock" icon but ensure the user cannot type into these fields.
- **Image Management Section**:
  - **Grid Display**: Show current images from `product.images`.
  - **Removal**: Add a "Delete" overlay/button on each thumbnail. Removal will involve updating the product via the existing `updateProduct` (PUT) with a filtered image list.
  - **Upload Flow**: 
    - Add an "Add Image" button.
    - Wire it to a hidden `<input type="file" accept="image/*">`.
    - Trigger `uploadProductImage` on selection.
    - Update the local `formData.images` and `products` state upon successful upload.

### 3. Backend: Permission Reinforcement
- **Note**: The backend `UpdateProductService` already enforces the `MANAGER` role for enrichment. No changes required to backend logic, but the UI must align with the "Manager cannot edit ERP fields" business rule.

## UI/UX Standards (FAANG Level)
- **State Integrity**: Ensure the product list reflects image changes immediately after upload/removal.
- **Feedback**: Show a loading spinner during image uploads.
- **Error Handling**: Gracefully handle 400/500 errors from the image pipeline (e.g., file too large, invalid format).
- **UX**: Use a clean, modern grid for images with hover effects for management actions.

## Verification Plan
1. **Permission Test**: Attempt to type in the Name/Price/Stock fields and verify they are locked.
2. **Upload Test**: Upload a valid image and verify it appears in the list and is persisted.
3. **Removal Test**: Remove an image and verify it is removed from the UI and the backend.
4. **Resiliency Test**: Attempt to upload a non-image file and verify the error message is displayed.
