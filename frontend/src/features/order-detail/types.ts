import type { OrderStatus, OrderItem } from "@/entities/order/model/types";
import type { CustomerReturnStatus } from "@/entities/pickpoint";

export interface MyOrderDetailReturn {
  id: number;
  status: CustomerReturnStatus;
  reason: string;
  createdAt: string;
  refundAmount: number | null;
  items: Array<{ productName: string; quantity: number; price: number }>;
}

export interface MyOrderDetail {
  id: number;
  status: OrderStatus;
  totalAmount: number;
  items: OrderItem[];
  createdAt: string;
  loyaltyPointsRedeemed: number;
  loyaltyPointsAwarded: number;
  deliveryType: "PICKPOINT" | "HOME" | null;
  deliveryAddress: string | null;
  pickpointName: string | null;
  pickpointAddress: string | null;
  trackingNumber: string | null;
  estimatedDeliveryDate: string | null;
  courierName: string | null;
  // Extended fields — absent until backend extends OrderDto; gracefully degrade to undefined
  shippedAt?: string | null;
  deliveredAt?: string | null;
  cancelledAt?: string | null;
  refundedAt?: string | null;
  outForDeliveryAt?: string | null;
  deliveryAttemptedAt?: string | null;
  deliveryAttemptReason?: string | null;
  deliveryAttemptCount?: number;
  customerReturn?: MyOrderDetailReturn | null;
}
