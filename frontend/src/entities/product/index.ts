// ── Public API of the product entity slice ──────────────────────
// Every import from outside this slice must go through this barrel.

export type { Product, ProductImageResponse } from "./model/types";
export { default as ProductCard } from "./ui/ProductCard";
export { default as ProductDetail } from "./ui/ProductDetail";
export { default as ProductCardSkeleton } from "./ui/ProductCardSkeleton";
export { default as ProductDetailSkeleton } from "./ui/ProductDetailSkeleton";
export { default as StockBadge } from "./ui/StockBadge";
export { fetchProductByErpId } from "./api";
