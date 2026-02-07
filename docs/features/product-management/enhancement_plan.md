# Enhancement Plan: Product Management Dashboard

This plan outlines the steps required to add product images and search functionality to the manager dashboard, following senior-level engineering standards.

## Proposed Changes

### 1. Frontend: Manage Products Page
- **File**: `frontend/src/app/(admin)/manage/page.tsx`
- **Search Implementation**:
  - Integrate a debounced search input (reusing patterns from `features/search`).
  - Initially implement client-side filtering for immediate feedback on the current 100-item page.
  - Plan for server-side integration once the backend supports query parameters.
- **Image Integration**:
  - Modify the `Table` component to include a "Thumbnail" column.
  - Use `next/image` with optimized dimensions (e.g., 40x40px).
  - Implement a fallback placeholder using `Lucide` icons or the project's "No image" pattern.

### 2. Frontend: API Layer
- **File**: `frontend/src/features/products/api.ts`
- **Change**: Update `getProducts` to accept an optional `search` string parameter to support future server-side filtering.

### 3. Backend: Product Controller (Optional/Recommended)
- **File**: `backend/src/main/java/tj/radolfa/infrastructure/web/ProductController.java`
- **Change**: Update the `getAllProducts` endpoint to accept a `search` query parameter and apply it to the `loadAll()` results before pagination.

## UI/UX Standards (FAANG Level)
- **Consistency**: Use existing design tokens and components (Shadcn/UI).
- **Performance**: Debounce search input (300ms) to prevent excessive filtering/re-renders.
- **Accessibility**: Ensure Alt text for images and proper ARIA labels for search.
- **Robustness**: Handle loading states and empty search results gracefully.

## Verification Plan
1. **Visual Check**: Verify that images appear in the table and align correctly.
2. **Search Test**: Type a product name in the search bar and verify that the list filters in real-time.
3. **Empty States**: Verify the UI when no products match the search query.
4. **API Integrity**: Ensure that existing pagination and CRUD operations still function correctly.
