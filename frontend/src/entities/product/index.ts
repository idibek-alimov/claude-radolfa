// ── Public API of the product entity slice ──────────────────────
// Every import from outside this slice must go through this barrel.

export type {
    Sku,
    ListingVariant,
    SiblingVariant,
    ListingVariantDetail,
    PaginatedListings,
    HomeSection,
    CollectionPage,
} from "./model/types";
export { default as ProductCard } from "./ui/ProductCard";
export { default as ProductDetail } from "./ui/ProductDetail";
export { default as ProductCardSkeleton } from "./ui/ProductCardSkeleton";
export { default as ProductDetailSkeleton } from "./ui/ProductDetailSkeleton";
export { default as StockBadge } from "./ui/StockBadge";
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
