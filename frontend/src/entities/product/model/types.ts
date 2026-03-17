// ── Storefront types – 3-Tier Product Hierarchy ─────────────────
//
// These interfaces mirror the backend DTOs exposed by ListingController:
//   ListingVariantDto, ListingVariantDetailDto, SkuDto
//
// The Admin panel uses a separate Product type in features/products/types.ts.

/**
 * A purchasable unit — one size of one colour variant.
 * Displayed on the product detail page as a size selector.
 */
export interface Sku {
  id: number;
  erpItemCode: string;
  sizeLabel: string;
  stockQuantity: number;
  originalPrice: number;
  discountedPrice: number | null;
  loyaltyPrice: number | null;
  discountPercentage: number | null;
  loyaltyDiscountPercentage: number | null;
  saleTitle: string | null;
  saleColorHex: string | null;
  onSale: boolean;
  discountedEndsAt: string | null;
}

/**
 * Product card shown on the grid/listing page.
 * One card per colour variant (e.g. "T-Shirt — Red").
 *
 * Aggregate pricing fields (originalPrice, discountedPrice, etc.) and
 * totalStock are computed server-side from the variant's SKUs.
 */
export interface ListingVariant {
  id: number;
  slug: string;
  name: string;
  category: string | null;
  colorKey: string;
  colorHexCode: string | null;
  webDescription: string | null;
  images: string[];
  originalPrice: number;
  discountedPrice: number | null;
  loyaltyPrice: number | null;
  discountPercentage: number | null;
  loyaltyDiscountPercentage: number | null;
  saleTitle: string | null;
  saleColorHex: string | null;
  totalStock: number;
  topSelling: boolean;
  featured: boolean;
}

/**
 * A single product attribute shown on the detail page.
 * Examples: key="Material" value="Organic Wool", key="Fit" value="Oversized".
 */
export interface Attribute {
  key: string;
  value: string;
}

/**
 * Lightweight reference to another colour of the same product.
 * Enables frontend colour swatches without a second API call.
 */
export interface SiblingVariant {
  slug: string;
  colorKey: string;
  colorHexCode: string | null;
  thumbnail: string | null;
}

/**
 * Full detail view for a single listing variant.
 * Includes the SKU list (sizes/prices) and sibling colour swatches.
 */
export interface ListingVariantDetail extends ListingVariant {
  attributes: Attribute[];
  skus: Sku[];
  siblingVariants: SiblingVariant[];
}

/**
 * Paginated response returned by the listings API.
 * Mirrors backend `PageResult<ListingVariantDto>`.
 */
export interface PaginatedListings {
  items: ListingVariant[];
  totalElements: number;
  page: number;
  hasMore: boolean;
}

/**
 * A single homepage collection row (e.g. "Featured", "New Arrivals").
 * Mirrors backend `HomeSectionDto`.
 */
export interface HomeSection {
  key: string;
  title: string;
  items: ListingVariant[];
}

/**
 * Paginated response for a single collection's "View All" page.
 * Mirrors backend `CollectionPageDto`.
 */
export interface CollectionPage {
  key: string;
  title: string;
  page: PaginatedListings;
}

/**
 * Category tree node returned by GET /api/v1/categories.
 */
export interface CategoryTree {
  id: number;
  name: string;
  slug: string;
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
