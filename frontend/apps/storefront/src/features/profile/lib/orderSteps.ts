import type { MyOrder } from "@/entities/order";

/**
 * Exception statuses (file 01 Phase 4) — rendered as a muted status note,
 * never a stepper.
 */
export const EXCEPTION_STATUSES = [
  "CANCELLED",
  "REFUNDED",
  "RETURN_INITIATED",
  "RETURNED_TO_WAREHOUSE",
  "RECALL_REQUESTED",
] as const;

export type OrderRenderKind = "stepper" | "delivered" | "exception";

/** Which card layout an order gets — design-faithful (Resolved Decision, Phase 8). */
export function orderRenderKind(order: MyOrder): OrderRenderKind {
  if ((EXCEPTION_STATUSES as readonly string[]).includes(order.status)) return "exception";
  if (order.status === "DELIVERED") return "delivered";
  return "stepper";
}

export interface OrderStep {
  labelKey: string;
  done: boolean;
  current: boolean;
}

/**
 * 5-step stepper for in-progress orders — branches on `deliveryType`.
 * A step is "done" when its backing timestamp is non-null; the last done
 * step is marked "current" (matches the design's magenta `.cur` dot).
 */
export function buildOrderSteps(order: MyOrder): OrderStep[] {
  const isPickup = order.deliveryType === "PICKPOINT";

  const done = [
    true, // Placed — the order exists
    order.status !== "PENDING",
    Boolean(order.shippedAt || order.claimedAt),
    Boolean(isPickup ? order.readyForPickupAt : order.outForDeliveryAt),
    Boolean(order.deliveredAt),
  ];

  const labelKeys = [
    "orderPlaced",
    "statusPaid",
    "orderShipped",
    isPickup ? "orderReadyForPickup" : "orderOutForDelivery",
    isPickup ? "orderCollected" : "orderDelivered",
  ];

  let currentIndex = 0;
  done.forEach((d, i) => {
    if (d) currentIndex = i;
  });

  return labelKeys.map((labelKey, i) => ({
    labelKey,
    done: done[i],
    current: i === currentIndex,
  }));
}
