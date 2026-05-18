export interface Pickpoint {
  id: number;
  name: string;
  address: string;
  active: boolean;
  latitude: number | null;
  longitude: number | null;
  hasParking: boolean;
  hasFittingRoom: boolean;
  hasCardPayment: boolean;
  wheelchairAccessible: boolean;
  timezone: string | null;
  temporarilyClosed: boolean;
  isOpenNow: boolean;
}

export interface CreatePickpointPayload {
  name: string;
  address: string;
  latitude: number | null;
  longitude: number | null;
  hasParking: boolean;
  hasFittingRoom: boolean;
  hasCardPayment: boolean;
  wheelchairAccessible: boolean;
}

export interface UpdatePickpointPayload {
  name: string;
  address: string;
  active: boolean;
  latitude: number | null;
  longitude: number | null;
  hasParking: boolean;
  hasFittingRoom: boolean;
  hasCardPayment: boolean;
  wheelchairAccessible: boolean;
  timezone: string | null;
  temporarilyClosed: boolean;
}

export interface PickpointHours {
  id: number;
  dayOfWeek: number; // 1=Mon … 7=Sun
  openTime: string;  // "HH:mm"
  closeTime: string; // "HH:mm"
}

export interface UpsertPickpointHoursPayload {
  dayOfWeek: number;
  openTime: string;
  closeTime: string;
}

// ── Customer Return types ─────────────────────────────────────────────────────

export type ReturnReason =
  | "DAMAGED"
  | "WRONG_ITEM"
  | "NOT_AS_DESCRIBED"
  | "CHANGED_MIND"
  | "OTHER";

export type CustomerReturnStatus =
  | "RECEIVED"
  | "SENT_TO_WAREHOUSE"
  | "REFUND_APPROVED"
  | "REFUNDED";

export interface CustomerReturnItem {
  orderItemId: number;
  productName: string | null;
  skuCode: string | null;
  quantity: number;
  unitPrice: number;
  refundAmount: number;
  reason: ReturnReason;
  notes: string | null;
}

export interface CustomerReturn {
  id: number;
  orderId: number;
  customerName: string | null;
  customerPhone: string | null;
  status: CustomerReturnStatus;
  receivedAt: string;
  sentToWarehouseAt: string | null;
  items: CustomerReturnItem[];
  notes: string | null;
  totalRefundAmount: number;
}

// Shape returned by GET /api/v1/pickpoint/orders/{id}/for-return
export interface ReturnableOrder {
  orderId: number;
  userId: number;
  items: ReturnableItem[];
}

export interface ReturnableItem {
  orderItemId: number;
  productName: string;
  skuCode: string | null;
  quantity: number;
  unitPrice: number;
}

export interface CreateCustomerReturnPayload {
  orderId: number;
  notes?: string;
  items: {
    orderItemId: number;
    quantity: number;
    reason: ReturnReason;
    notes?: string;
  }[];
}
