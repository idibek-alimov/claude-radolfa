import type { PaginatedResponse } from "@/shared/api/types";
import type { Tag } from "@/entities/tag";

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
  productBaseId: number;
  variantId: number;
  productCode: string;
  slug: string;
  colorKey: string;
  colorDisplayName: string;
  colorHex: string | null;
  categoryName: string | null;
  webDescription: string | null;
  images: string[];
  tags: Tag[];
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
  values: string[];
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
  categoryId:    number | null;
  weightKg:      number | null;
  widthCm:       number | null;
  heightCm:      number | null;
  depthCm:       number | null;
  attributes:    Attribute[];
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

// ── Admin product-card read model ─────────────────────────────────────────────
// Mirrors ProductCardDto from the backend.

/** Full admin read model for a product base and all its color variants. */
export interface ProductCard {
  productBaseId: number;
  name: string;
  brand: string | null;
  categoryId: number | null;
  categoryName: string | null;
  variants: ProductCardVariant[];
}

/** One color variant inside a ProductCard. */
export interface ProductCardVariant {
  variantId: number;
  slug: string;
  productCode: string;
  colorId: number;
  colorKey: string;
  colorDisplayName: string;
  colorHex: string;
  webDescription: string | null;
  images: ProductCardImage[];
  attributes: { key: string; values: string[] }[];
  tags: Tag[];
  skus: ProductCardSku[];
  isEnabled: boolean;
  isActive: boolean;
  weightKg: number | null;
  widthCm: number | null;
  heightCm: number | null;
  depthCm: number | null;
}

/** Image reference — id is needed to call the reorder endpoint. */
export interface ProductCardImage {
  id: number;
  url: string;
}

/** SKU in admin context — raw pricing, no discount/loyalty fields. */
export interface ProductCardSku {
  skuId: number;
  skuCode: string;
  sizeLabel: string;
  stockQuantity: number;
  originalPrice: number;
}
