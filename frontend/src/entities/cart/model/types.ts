export interface CartItem {
  skuId: number;
  skuCode: string;
  productName: string;
  colorName: string;
  sizeLabel: string;
  imageUrl: string | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  availableStock: number;
  inStock: boolean;
}

export interface Cart {
  cartId: number;
  items: CartItem[];
  totalAmount: number;
  itemCount: number;
  couponCode?: string;
}

export interface ApplyCouponResponse {
  valid: boolean;
  discountId?: number;
  affectedSkus: string[];
  invalidReason?: "NOT_FOUND" | "DISABLED" | "NOT_ACTIVE" | "CAP_EXHAUSTED" | "NO_ITEMS_AFFECTED";
  cart: Cart;
}
