import type { PaginatedResponse } from "@/shared/api/types";

/**
 * A purchasable unit — one size of one colour variant.
 * Displayed on the product detail page as a size selector.
 *
 * Each SKU carries its own full pricing block. When the user selects a size
 * on the detail page, swap to that SKU's pricing (discount may appear or
 * disappear depending on whether the size is part of an active campaign).
 */
export interface Sku {
  skuId: number;
  skuCode: string;
  sizeLabel: string;
  stockQuantity: number;
  // Pricing — mirrors backend SkuDto exactly
  originalPrice: number;
  discountPrice: number | null;      // null = no active discount on this SKU
  discountPercentage: number | null; // whole number, e.g. 20 means 20%
  discountName: string | null;
  discountColorHex: string | null;
  loyaltyPrice: number | null;       // null for guests / users without a tier
}

/**
 * Product card shown on the grid/listing page.
 * One card per colour variant (e.g. "T-Shirt — Red").
 *
 * Pricing fields reflect the cheapest-discounted SKU's data at the variant level.
 * loyaltyPrice / loyaltyPercentage are null for guests and users without a tier.
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
  skus: Sku[];
  // Pricing — mirrors backend ListingVariantDto exactly
  originalPrice: number;              // pre-discount price of cheapest SKU; always present
  discountPrice: number | null;       // null = no active sale on this variant
  discountPercentage: number | null;  // whole number, null if no sale
  discountName: string | null;        // e.g. "Winter Collection", null if no sale
  discountColorHex: string | null;    // badge background color, null if no sale
  loyaltyPrice: number | null;        // null for guests / no-tier users
  loyaltyPercentage: number | null;   // user's own tier %, null for guests / no-tier
  isPartialDiscount: boolean;         // true = only some sizes are on sale
}

/**
 * A single product attribute shown on the detail page.
 */
export interface Attribute {
  key: string;
  value: string;
}

/**
 * A sibling colour variant of the same base product.
 * Used to render the colour switcher on the detail page.
 */
export interface SiblingVariant {
  slug: string;
  colorKey: string;
  colorHex: string | null;
  thumbnail: string | null;
}

/**
 * Full detail view for a single listing variant.
 * Inherits all pricing fields from ListingVariant.
 */
export interface ListingVariantDetail extends ListingVariant {
  productBaseId: number;
  categoryId: number | null;
  webDescription: string | null;
  attributes: Attribute[];
  siblingVariants: SiblingVariant[];
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
