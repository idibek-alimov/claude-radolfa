// Synced from backend — do not edit manually
export type OrderStatus = "PENDING" | "PAID" | "SHIPPED" | "READY_FOR_PICKUP" | "DELIVERED" | "CANCELLED";
export type DeliveryType = "HOME" | "PICKPOINT";

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

/** Admin-only item shape — adds currentStock for pre-fulfillment availability check. */
export interface AdminOrderItem extends DeliveredOrderItem {
  currentStock: number | null;
}

/** Minimal order shape — only what the review form needs. */
export interface DeliveredOrder {
  id: number;
  status: string;
  totalAmount: number;
  createdAt: string;
  items: DeliveredOrderItem[];
  loyaltyPointsRedeemed: number;
  loyaltyPointsAwarded: number;
}

/** Row returned by GET /api/v1/admin/orders (list). */
export interface AdminOrderListItem {
  id: number;
  userPhone: string;
  userName: string | null;
  createdAt: string;
  status: OrderStatus;
  totalAmount: number;
  deliveryType: DeliveryType | null;
  itemCount: number;
}

/** Full order returned by GET /api/v1/admin/orders/{id}. */
export interface AdminOrderDetail {
  id: number;
  userPhone: string;
  userName: string | null;
  status: OrderStatus;
  totalAmount: number;
  createdAt: string;
  loyaltyPointsRedeemed: number;
  loyaltyPointsAwarded: number;
  items: AdminOrderItem[];
  deliveryType: DeliveryType | null;
  deliveryAddress: string | null;
  preferredTimeWindow: string | null;
  pickpointId: number | null;
  pickpointName: string | null;
  pickpointAddress: string | null;
  courierName: string | null;
  trackingNumber: string | null;
  estimatedDeliveryDate: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
  cancelledAt: string | null;
}

export interface RecentOrder {
  orderId: number;
  userPhone: string;
  totalAmount: number;
  status: string;
  createdAt: string;
}

export interface AdminOrderSummary {
  totalOrders: number;
  todayOrders: number;
  revenueToday: number;
  revenueThisMonth: number;
  recentOrders: RecentOrder[];
}
