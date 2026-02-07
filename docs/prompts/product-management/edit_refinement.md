# Professional Claude Code Instruction Prompt: Product Edit Refinement

### PERSONA
You are a **Senior Frontend Engineer at a FAANG company**. You are tasked with refining an enterprise Product Management dashboard to enforce data ownership and provide a premium media management experience.

### TASK
Refine the **Product Edit Dialog** to strictly enforce field-level permissions and implement **Image Upload & Management** functionality.

### CONSTRAINTS (CRITICAL)
- **Data Ownership**: ERPNext is the source of truth for core product data. Managers are only allowed to **enrich** products, not modify core ERP fields.
- **Zero Exploration**: Do not search the whole project. Use only the provided file paths.
- **Media Management**: Managers must be able to upload new images and remove existing ones.

### TARGET FILES
1. **Logic & UI**: `frontend/src/app/(admin)/manage/page.tsx`
2. **API Definition**: `frontend/src/features/products/api.ts`
3. **Backend Reference (API Surface)**: 
   - `backend/src/main/java/tj/radolfa/infrastructure/web/ProductImageController.java` (Upload endpoint)
   - `backend/src/main/java/tj/radolfa/infrastructure/web/dto/UpdateProductRequestDto.java` (Payload structure)

### REQUIREMENTS

#### 1. Enforce Field Locks (ERP vs Manager)
- In the edit dialog of `page.tsx`, set the following fields to **Read-Only / Disabled**:
  - **ERP ID**
  - **Name**
  - **Price**
  - **Stock**
- Visually indicate that these are locked (keep the Lock icon, use a `muted` or `bg-slate-50` background).
- Ensure the following remain **Editable**:
  - **Web Description**
  - **Top Selling Toggle**

#### 2. Advanced Image Management UI
- **Image Grid**: Introduce a 3 or 4-column grid within the dialog to display current product images.
- **Removal**: Each image should have a "Remove" button (Trash icon). 
  - *Implementation Strategy*: Removal should update the local `formData` and call the existing `updateProduct` (PUT) endpoint with the updated list of URLs.
- **Professional Upload flow**:
  1. Add an **"Add Image"** button.
  2. Implement an `uploadProductImage` function in `api.ts` which hits `POST /api/v1/products/{erpId}/images` using `multipart/form-data`.
  3. When an image is selected via a hidden file input:
     - Show a loading state/overlay on the image grid.
     - Upon successful upload, update both the `formData.images` and the global `products` list state with the new list returned by the server.
     - Show a Toast/Notification for success/failure.

### ENGINEERING STANDARDS
- **State Management**: Perform optimistic UI updates where appropriate, or ensure clean re-fetching after mutations.
- **Robustness**: Properly handle `multipart/form-data` and 5MB size limits as defined in the backend.
- **UX**: Use Shadcn/UI components (Button, Progress, Toast) to make the upload feel premium.
