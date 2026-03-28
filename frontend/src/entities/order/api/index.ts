import apiClient from "@/shared/api/axios";
import type { DeliveredOrder } from "../model/types";

/** Fetch authenticated user's delivered orders — used for review submission. */
export const fetchMyDeliveredOrders = (): Promise<DeliveredOrder[]> =>
  apiClient
    .get("/api/v1/orders/my-orders")
    .then((r) =>
      (r.data as DeliveredOrder[]).filter((o) => o.status === "DELIVERED")
    );
