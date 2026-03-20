import type { PaginatedResponse } from "@/shared/api/types";

/**
 * A purchasable unit — one size of one colour variant.
 * Displayed on the product detail page as a size selector.
 */
export interface Sku {
  skuId: number;
  skuCode: string;
  sizeLabel: string;
  stockQuantity: number;
  price: number;
}

/**
 * Product card shown on the grid/listing page.
 * One card per colour variant (e.g. "T-Shirt — Red").
 */
export interface ListingVariant {
  variantId: number;
  productCode: string;
  slug: string;
  colorKey: string;
  colorDisplayName: string;
  colorHex: string | null;
  categoryName: string | null;
  topSelling: boolean;
  featured: boolean;
  images: string[];
  minPrice: number;
  maxPrice: number;
  tierDiscountedMinPrice: number | null;
  skus: Sku[];
}

/**
 * A single product attribute shown on the detail page.
 */
export interface Attribute {
  key: string;
  value: string;
}

/**
 * Full detail view for a single listing variant.
 */
export interface ListingVariantDetail extends ListingVariant {
  productBaseId: number;
  categoryId: number | null;
  webDescription: string | null;
  attributes: Attribute[];
}

/** Paginated response for listings — alias of the shared generic. */
export type PaginatedListings = PaginatedResponse<ListingVariant>;

/**
 * A single homepage collection row (e.g. "Featured", "New Arrivals").
 */
export interface HomeSection {
  key: string;
  title: string;
  listings: ListingVariant[];
}

/**
 * Single collection page — returns all items (not paginated server-side).
 */
export interface CollectionPage {
  key: string;
  title: string;
  listings: ListingVariant[];
}

/**
 * Category tree node returned by GET /api/v1/categories.
 */
export interface CategoryTree {
  id: number;
  name: string;
  slug: string;
  parentId: number | null;
  children: CategoryTree[];
}

/**
 * Color entry returned by GET /api/v1/colors.
 */
export interface Color {
  id: number;
  colorKey: string;
  displayName: string | null;
  hexCode: string | null;
}
