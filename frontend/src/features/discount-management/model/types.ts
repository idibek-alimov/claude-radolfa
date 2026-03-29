/** A discount type (tier/rank of discount — e.g. "Flash Sale", "Seasonal"). */
export interface DiscountType {
  id: number;
  name: string;
  rank: number; // lower = higher priority when multiple discounts apply
}

/** Full discount response from the backend. */
export interface DiscountResponse {
  id: number;
  type: DiscountType;
  itemCodes: string[]; // SKU codes this discount applies to
  discountValue: number; // percentage — e.g. 20 means 20%
  validFrom: string; // ISO instant
  validUpto: string; // ISO instant
  disabled: boolean;
  title: string; // campaign display name e.g. "Winter Sale"
  colorHex: string; // badge background color, 6-char hex no # (e.g. "E74C3C")
}

/** Request body for POST /api/v1/admin/discounts and PUT /api/v1/admin/discounts/{id} */
export interface DiscountFormValues {
  typeId: number;
  itemCodes: string[];
  discountValue: number;
  validFrom: string; // ISO instant
  validUpto: string; // ISO instant
  title: string;
  colorHex: string; // 6-char hex, no #
}

/** Filters for the paginated discount list */
export interface DiscountListFilters {
  typeId?: number;
  status?: "active" | "disabled" | "all";
  from?: string;
  to?: string;
  page: number;
  size: number;
}

/** Request body for POST /api/v1/admin/discount-types and PUT /api/v1/admin/discount-types/{id} */
export interface DiscountTypeFormValues {
  name: string;
  rank: number;
}
