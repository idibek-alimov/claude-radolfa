# Frontend Refactor Plan: Entity-Based Categories & Colors

This report outlines the necessary changes to transition the frontend from using flat strings for categories and colors to using full entity objects. This enables hierarchical breadcrumbs, hex-based color swatches, and more robust URL logic.

## 1. Backend DTO & Mapping Updates
To support the UI requirements, the backend DTOs must be updated to return nested objects instead of flattened strings.

### DTO Changes
- **ListingVariantDto**: Replace `String category` and `String colorKey` with `CategoryDto` (or a flat but more descriptive `CategoryView`) and `ColorDto`.
- **ListingVariantDetailDto**: Similar update, ensuring the category includes its parent for breadcrumb traversal.

### Mapping (ListingReadAdapter)
- Update `toGridDto` and `toDetailDto` to fetch the full `Category` and `Color` entities from the `ListingVariantEntity` / `ProductBaseEntity`.

## 2. Frontend Model Updates (`entities/product/model/types.ts`)
Update the TypeScript interfaces to match the new DTO structure:

```typescript
export interface Category {
  id: number;
  name: string;
  slug: string;
  parentId?: number;
  parent?: Category; // Needed for breadcrumb traversal
}

export interface ListingVariant {
  ...
  category: Category;
  color: Color;
  ...
}
```

## 3. Component Updates (HTML Fragments)

### Product Cards (`entities/product/ui/ProductCard.tsx`)
- **Property Path**: Change `{listing.colorKey}` to `{listing.color.name}`.
- **Color Swatch**: Add a small circular swatch div with `backgroundColor: listing.color.hexCode`.
- **Logic**: Use `listing.category.slug` for any category-specific links.

### Product Detail Page (`entities/product/ui/ProductDetail.tsx`)
- **Breadcrumbs**: 
    - Replace the static "Products > Name" breadcrumb.
    - Implement a helper `getCategoryPath(category)` to recursively build the path: `Home > [Parent Category] > [Child Category] > Product Name`.
- **Color Swatches**:
    - Update the `allSwatches` mapping to use `swatch.color.hexCode` for the background color instead of just a text label or thumbnail.
    - Property path fix: `${variant.colorKey}` -> `${variant.color.displayName}`.

### Navigation Sidebar (New Widget or `CatalogSection.tsx`)
- **Requirement**: A nested menu for categories.
- **Implementation**:
    - Fetch the category tree from `GET /api/v1/categories`.
    - Create a recursive `CategoryMenuItem` component.
    - Use Framer Motion for smooth expansion/collapsing of sub-categories.

### Catalog Section (`widgets/ProductList/ui/CatalogSection.tsx`)
- **Breadcrumbs**: Update to show the current category context (e.g., if filtered by category).
- **Layout**: Introduce a sidebar on larger screens to house the new nested category menu.

## 4. URL & Navigation Logic
- **Slug-Based Routing**: All navigation to categories must use `/products?category=${category.slug}` or a cleaner `/category/${category.slug}` pattern.
- **Breadcrumb Links**: Ensure breadcrumb items link to the correct category slug.

## 5. Summary of Affected Files
| File Path | Change Type | Note |
|-----------|-------------|------|
| `backend/.../ListingVariantDto.java` | DTO Update | Switch strings to objects |
| `backend/.../ListingReadAdapter.java` | Mapping | Populate objects from entities |
| `frontend/.../types.ts` | Type Update | Update interfaces |
| `frontend/.../ProductCard.tsx` | UI | Add color swatch, fix paths |
| `frontend/.../ProductDetail.tsx` | UI | Dynamic breadcrumbs, hex swatches |
| `frontend/.../CatalogSection.tsx` | UI/Layout | Add Sidebar, Update Breadcrumbs |
| `frontend/.../Sidebar.tsx` (NEW) | UI | Recursive category menu |
