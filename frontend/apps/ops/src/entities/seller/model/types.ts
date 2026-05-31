/** Seller profile. Mirrors SellerDto from the backend. */
export interface Seller {
  id: number;
  userId: number;
  shopName: string;
  logoUrl: string | null;
  bio: string | null;
  createdAt: string;
}

/** One row in the seller order-items view. Mirrors SellerOrderItemDto. */
export interface SellerOrderItem {
  orderItemId: number;
  orderId: number;
  orderStatus: string;
  orderCreatedAt: string;
  productName: string;
  skuCode: string;
  quantity: number;
  price: number;
}
