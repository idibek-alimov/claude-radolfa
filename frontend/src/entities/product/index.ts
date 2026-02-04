// ── Public API of the product entity slice ──────────────────────
// Every import from outside this slice must go through this barrel.
// Nothing else in this directory is importable by other slices.

export type { Product, ProductImageResponse } from "./model/types";
export { default as ProductCard } from "./ui/ProductCard";
export { default as ProductDetail } from "./ui/ProductDetail";
export { fetchProductByErpId } from "./api";
