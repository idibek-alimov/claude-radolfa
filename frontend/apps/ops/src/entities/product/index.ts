// ── Public API of the product entity slice ──────────────────────
// Every import from outside this slice must go through this barrel.

export type {
    Sku,
    Attribute,
    ListingVariant,
    ListingVariantDetail,
    PaginatedListings,
    HomeSection,
    CollectionPage,
    CategoryTree,
    Color,
    AdminProductRow,
} from "./model/types";
export { ProductStatus } from "./model/types";
export { ProductStatusBadge } from "./ui/ProductStatusBadge";
export { VariantTabBar } from "./ui/VariantTabBar";
export type { VariantTabBarItem } from "./ui/VariantTabBar";
export { ColorPickerDialog } from "./ui/ColorPickerDialog";
export { default as ProductCard } from "./ui/ProductCard";
// ProductDetail is storefront-only (imports cart + reviews-questions); not exported from ops entity.
export { default as ProductCardSkeleton } from "./ui/ProductCardSkeleton";
export { default as ProductDetailSkeleton } from "./ui/ProductDetailSkeleton";
export { default as StockBadge } from "./ui/StockBadge";
export { SkuPicker } from "./ui/SkuPicker";
export {
    fetchListings,
    fetchListingBySlug,
    searchListings,
    fetchAutocomplete,
    fetchHomeCollections,
    fetchCollectionPage,
    fetchCategoryProducts,
    fetchCategoryTree,
} from "./api";
