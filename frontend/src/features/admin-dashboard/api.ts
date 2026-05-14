import apiClient from "@/shared/api/axios";
import type { AdminOrderSummary } from "@/entities/order";

/** GET /api/v1/admin/orders/summary — ADMIN only */
export async function fetchAdminOrderSummary(): Promise<AdminOrderSummary> {
  const response = await apiClient.get<AdminOrderSummary>("/api/v1/admin/orders/summary");
  return response.data;
}
