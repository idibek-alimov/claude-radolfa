import { useQuery, useMutation, useQueryClient, keepPreviousData } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { DeliveredOrder, AdminOrderListItem, AdminOrderDetail, OrderStatus } from "../model/types";
import type { PaginatedResponse } from "@/shared/api/types";

/** Fetch authenticated user's delivered orders — used for review submission. */
export const fetchMyDeliveredOrders = (): Promise<DeliveredOrder[]> =>
  apiClient
    .get("/api/v1/orders/my-orders")
    .then((r) =>
      (r.data as DeliveredOrder[]).filter((o) => o.status === "DELIVERED")
    );

export function useAdminOrders(params: {
  page: number;
  search: string;
  status: OrderStatus | "";
  sortBy: string;
  sortDir: string;
  size: number;
}) {
  const { page, search, status, sortBy, sortDir, size } = params;
  return useQuery({
    queryKey: ["admin-orders", page, search, status, sortBy, sortDir, size],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<AdminOrderListItem>>("/api/v1/admin/orders", {
          params: {
            page, size, search,
            ...(status ? { status } : {}),
            sortBy,
            sortDir,
          },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
  });
}

export function useAdminOrder(orderId: number | null) {
  return useQuery({
    queryKey: ["admin-order", orderId],
    queryFn: () =>
      apiClient
        .get<AdminOrderDetail>(`/api/v1/admin/orders/${orderId}`)
        .then((r) => r.data),
    enabled: orderId !== null,
  });
}

export function useUpdateOrderStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      orderId,
      status,
      courierName,
      trackingNumber,
      estimatedDeliveryDate,
    }: {
      orderId: number;
      status: OrderStatus;
      courierName?: string;
      trackingNumber?: string;
      estimatedDeliveryDate?: string;
    }) =>
      apiClient.patch(`/api/v1/orders/${orderId}/status`, {
        status,
        courierName: courierName?.trim() || undefined,
        trackingNumber: trackingNumber?.trim() || undefined,
        estimatedDeliveryDate: estimatedDeliveryDate || undefined,
      }),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ["admin-orders"] });
      qc.invalidateQueries({ queryKey: ["admin-order", vars.orderId] });
      qc.invalidateQueries({ queryKey: ["my-orders"] });
    },
  });
}
