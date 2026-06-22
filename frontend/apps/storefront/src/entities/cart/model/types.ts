export interface CartItem {
  skuId: number;
  skuCode: string;
  productName: string;
  colorName: string;
  sizeLabel: string;
  imageUrl: string | null;
  /** Final (discounted) unit price — the best-of campaign vs loyalty price. */
  unitPrice: number;
  quantity: number;
  /** unitPrice × quantity. */
  lineTotal: number;
  availableStock: number;
  inStock: boolean;
  /** Pre-discount snapshot price — show as strikethrough when greater than unitPrice. */
  originalUnitPrice: number;
  /** Realised discount %, null when mechanism is NONE. */
  discountPercent: number | null;
  /** Which pricing mechanism produced unitPrice. Exactly one ever applies. */
  mechanism: "NONE" | "CAMPAIGN" | "LOYALTY";
  category: string | null;
  /** Links to /products/{productCode}. Null for unsaved variants. */
  productCode: string | null;
}

export interface Cart {
  cartId: number;
  items: CartItem[];
  /** Discounted total — equals what checkout would charge right now. */
  totalAmount: number;
  itemCount: number;
  couponCode?: string;
  pendingOrderId?: number | null;
  /** Sum of originalUnitPrice × qty across all lines. */
  subtotal: number;
  /** Sum of savings on lines where a campaign discount won. */
  itemDiscounts: number;
  /** Sum of savings on lines where the loyalty tier discount won. */
  crownTier: number;
  /** Always 0 — shipping is free today. */
  shipping: number;
  /** itemDiscounts + crownTier. */
  savings: number;
}

export interface ApplyCouponResponse {
  valid: boolean;
  discountId?: number;
  affectedSkus: string[];
  invalidReason?: "NOT_FOUND" | "DISABLED" | "NOT_ACTIVE" | "CAP_EXHAUSTED" | "NO_ITEMS_AFFECTED";
  cart: Cart;
}
