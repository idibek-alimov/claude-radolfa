// ── Public API of the ProductList widget slice ──────────────────
export { default as ProductGrid } from "./ui/ProductGrid";
export { default as TopSellingSection } from "./ui/TopSellingSection";
export { default as CatalogSection } from "./ui/CatalogSection";
export type { ProductListProps } from "./model/types";
export { fetchListings } from "@/entities/product";
export type { PaginatedListings } from "@/entities/product";
