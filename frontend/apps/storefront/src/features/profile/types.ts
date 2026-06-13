import type { User } from "@radolfa/shared/user";
import type { OrderStatus, OrderItem, MyOrder, MyOrdersSummary } from "@/entities/order/model/types";
import type { CustomerReturnStatus } from "@/entities/pickpoint";

export type { User, OrderStatus, OrderItem, MyOrdersSummary };

/** Order shape consumed by the profile feature — re-exports the canonical
 *  `MyOrder` from `entities/order` (single source of truth, incl. stepper
 *  timestamps and `deliveryCode`). */
export type Order = MyOrder;

export interface UpdateProfileRequest {
    name: string;
    email: string;
}

export interface MyReturnItem {
  productName: string;
  quantity: number;
  refundAmount: number;
  reason: string;
}

export interface MyReturn {
  returnId: number;
  orderId: number;
  status: CustomerReturnStatus;
  receivedAt: string;
  sentToWarehouseAt: string | null;
  refundedAt: string | null;
  totalRefundAmount: number | null;
  items: MyReturnItem[];
}
