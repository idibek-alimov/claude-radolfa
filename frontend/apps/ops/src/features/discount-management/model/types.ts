export type AmountType = "PERCENT" | "FIXED";
export type Segment = "LOYALTY_TIER" | "NEW_CUSTOMER";

export interface DiscountTargetResponse {
  targetType: "SKU" | "CATEGORY" | "SEGMENT";
  referenceId: string;
  includeDescendants?: boolean;
}

export interface DiscountTargetInput {
  targetType: "SKU" | "CATEGORY" | "SEGMENT";
  referenceId: string;
  includeDescendants?: boolean;
}

export type StackingPolicy = "BEST_WINS" | "STACKABLE";

/** A discount type (tier/rank of discount — e.g. "Flash Sale", "Seasonal"). */
export interface DiscountType {
  id: number;
  name: string;
  rank: number; // lower = higher priority when multiple discounts apply
  stackingPolicy: StackingPolicy;
}

/** Full discount response from the backend. */
export interface DiscountResponse {
  id: number;
  type: DiscountType;
  targets: DiscountTargetResponse[];
  amountType: AmountType;
  amountValue: number;
  validFrom: string; // ISO instant
  validUpto: string; // ISO instant
  disabled: boolean;
  title: string; // campaign display name e.g. "Winter Sale"
  colorHex: string; // badge background color, 6-char hex no # (e.g. "E74C3C")
  minBasketAmount?: number;
  usageCapTotal?: number;
  usageCapPerCustomer?: number;
  couponCode?: string;
}

/** Request body for POST /api/v1/admin/discounts and PUT /api/v1/admin/discounts/{id} */
export interface DiscountFormValues {
  typeId: number;
  targets: DiscountTargetInput[];
  amountType: AmountType;
  amountValue: number;
  validFrom: string; // ISO instant
  validUpto: string; // ISO instant
  title: string;
  colorHex: string; // 6-char hex, no #
  minBasketAmount?: number;
  usageCapTotal?: number;
  usageCapPerCustomer?: number;
  couponCode?: string;
}

/** Filters for the paginated discount list */
export interface DiscountListFilters {
  typeId?: number;
  status?: "active" | "scheduled" | "expired" | "disabled" | "all";
  from?: string;
  to?: string;
  search?: string;
  sort?: string; // Spring convention: "field,asc" | "field,desc"
  page: number;
  size: number;
}

/** Request body for POST /api/v1/admin/discount-types and PUT /api/v1/admin/discount-types/{id} */
export interface DiscountTypeFormValues {
  name: string;
  rank: number;
  stackingPolicy: StackingPolicy;
}

/** Lightweight campaign summary embedded in discounted-product rows. */
export interface CampaignSummary {
  id: number;
  title: string;
  colorHex: string;
  amountValue: number;
  amountType: AmountType;
  type: DiscountType;
}

/** One row in the Discounted Products cross-campaign table. */
export interface DiscountedProductRow {
  skuId: number;
  skuCode: string;
  sizeLabel: string;
  stockQuantity: number;
  originalPrice: number;
  finalPrice: number;
  deltaPercent: number;
  winningCampaign: CampaignSummary;
  otherCampaigns: CampaignSummary[];
  productBaseId: number;
  productName: string;
  variantId: number;
  productCode: string;
  imageUrl: string;
}

/** Filters for GET /api/v1/admin/discounts/products */
export interface DiscountedProductFilters {
  search?: string;
  campaignId?: number;
  minDeltaPercent?: number;
  maxDeltaPercent?: number;
  sort?: string;
  page: number;
  size: number;
}

/** Filters for GET /api/v1/admin/discounts/{id}/skus */
export interface CampaignSkuFilters {
  search?: string;
  page: number;
  size: number;
}

/** One SKU code that is covered by two or more active campaigns simultaneously. */
export interface DiscountOverlapRow {
  skuCode: string;
  winningCampaign: CampaignSummary;
  losingCampaigns: CampaignSummary[];
}

// ── Analytics ─────────────────────────────────────────────────────

export interface DailyMetric {
  date: string; // ISO date yyyy-MM-dd
  orders: number;
  units: number;
  uplift: number;
}

export interface DiscountMetrics {
  ordersUsing: number;
  unitsMoved: number;
  revenueUplift: number;
  avgDiscountPerOrder: number;
  from: string;
  to: string;
  dailySeries: DailyMetric[];
}

export interface TopCampaignRow {
  campaign: CampaignSummary;
  ordersUsing: number;
  unitsMoved: number;
  revenueUplift: number;
}

export interface AnalyticsConfig {
  startDate: string; // ISO date yyyy-MM-dd
}
