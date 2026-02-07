# Professional Claude Code Instruction Prompt

Use this prompt to direct Claude to perform the enhancements. It is designed to minimize context-searching and maximize implementation quality.

---

### PERSONA
You are a **Senior Frontend Engineer at a FAANG company**. You specialize in building highly scalable, performant, and maintainable enterprise dashboards. You value code reuse, accessibility, and robust state management.

### TASK
Enhance the Product Management Dashboard with **Product Images** and **Real-time Search**.

### CONSTRAINTS & CONTEXT (READ FIRST)
- **Zero-Exploration Goal**: Do not analyze the entire project. Focus exclusively on the files provided.
- **Architecture**: The project follows a Feature-Sliced Design (FSD) inspired architecture.
- **Stack**: Next.js (App Router), TypeScript, Tailwind CSS, Lucide Icons, Shadcn/UI components.
- **Source Patterns**:
  - Image rendering: Re-use the pattern from `frontend/src/entities/product/ui/ProductCard.tsx`.
  - Search UI: Reference `frontend/src/features/search/ui/SearchBar.tsx` for debounce logic.

### TARGET FILES
1. **Logic & UI**: `frontend/src/app/(admin)/manage/page.tsx`
2. **API Definition**: `frontend/src/features/products/api.ts`
3. **Data Types**: `frontend/src/features/products/types.ts`
4. **Backend (Refactor Candidate)**: `backend/src/main/java/tj/radolfa/infrastructure/web/ProductController.java`

### REQUIREMENTS

#### 1. Product Thumbnails in List View
- **Column**: Insert a new column "Thumbnail" as the first column in the `Table` within `page.tsx`.
- **Implementation**: 
  - Use `next/image` to render `product.images[0]`.
  - Dimensions: 40x40px with a subtle `rounded-md` border.
  - Aspect Ratio: `aspect-square`.
  - Fallback: If no image exists, show a `Package` (Lucide) icon in a `muted` background box.
  - Performance: Use the `unoptimized` flag for S3 URLs as established in the project.

#### 2. Professional Search Functionality
- **UI**: Add a search input above the product table. Use the `Input` component with a `Search` icon prefix.
- **Logic**:
  - Implement a **300ms debounce** for the search state.
  - **Client-Side Filter**: Initially, filter the `products` array locally to provide instant feedback.
  - **Scalability**: Update `getProducts` in `api.ts` and `getAllProducts` in `ProductController.java` to accept an optional `search` query parameter. This ensures the implementation is "FAANG-ready" for larger datasets.
- **Empty State**: Ensure the "No products found" message updates contextually if a search yields zero results.

### DELIVERY CRITERIA
- Clean, type-safe TypeScript code.
- Minimal re-renders (use `useMemo` for filtered lists).
- Perfect alignment in the Shadcn/UI Table.
- No breaking changes to existing Create/Edit/Delete flows.
