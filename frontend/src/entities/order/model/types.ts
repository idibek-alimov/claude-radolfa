/** Minimal order item — only what the review form needs. */
export interface DeliveredOrderItem {
  productName: string;
  quantity: number;
  price: number;
  skuId: number | null;
  listingVariantId: number | null;
  imageUrl: string | null;
  skuCode: string | null;
  sizeLabel: string | null;
  slug: string | null;
  hasReviewed: boolean;
}

/** Minimal order shape — only what the review form needs. */
export interface DeliveredOrder {
  id: number;
  status: string;
  totalAmount: number;
  createdAt: string;   // ISO instant
  items: DeliveredOrderItem[];
  loyaltyPointsRedeemed: number;
  loyaltyPointsAwarded: number;
}
