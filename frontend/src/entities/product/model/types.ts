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
  /** Original / list price. */
  price: number;
  /** Effective price after promotions. */
  salePrice: number;
  onSale: boolean;
  saleEndsAt: string | null;
}

/**
 * Product card shown on the grid/listing page.
 * One card per colour variant (e.g. "T-Shirt — Red").
 *
 * Aggregate fields (priceStart, priceEnd, totalStock) are computed
 * server-side from the variant's SKUs.
 */
export interface ListingVariant {
  id: number;
  slug: string;
  name: string;
  colorKey: string;
  webDescription: string;
  images: string[];
  priceStart: number;
  priceEnd: number;
  totalStock: number;
  topSelling: boolean;
}

/**
 * Lightweight reference to another colour of the same product.
 * Enables frontend colour swatches without a second API call.
 */
export interface SiblingVariant {
  slug: string;
  colorKey: string;
  thumbnail: string;
}

/**
 * Full detail view for a single listing variant.
 * Includes the SKU list (sizes/prices) and sibling colour swatches.
 */
export interface ListingVariantDetail extends ListingVariant {
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
