import { useQuery, useMutation, useQueryClient, keepPreviousData } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { DeliveredOrder, AdminOrderListItem, AdminOrderDetail, AdminOrderSummary, OrderStatus } from "../model/types";
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
      courierId,
      trackingNumber,
      estimatedDeliveryDate,
    }: {
      orderId: number;
      status: OrderStatus;
      courierId?: number;
      trackingNumber?: string;
      estimatedDeliveryDate?: string;
    }) =>
      apiClient.patch(`/api/v1/orders/${orderId}/status`, {
        status,
        courierId: courierId ?? undefined,
        trackingNumber: trackingNumber?.trim() || undefined,
        estimatedDeliveryDate: estimatedDeliveryDate || undefined,
      }),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ["admin-orders"] });
      qc.invalidateQueries({ queryKey: ["admin-order", vars.orderId] });
      qc.invalidateQueries({ queryKey: ["admin-order-summary"] });
      qc.invalidateQueries({ queryKey: ["my-orders"] });
    },
  });
}

export function useAdminOrderSummary() {
  return useQuery({
    queryKey: ["admin-order-summary"],
    queryFn: () =>
      apiClient
        .get<AdminOrderSummary>("/api/v1/admin/orders/summary")
        .then((r) => r.data),
  });
}

export function useCancelOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId, reason }: { orderId: number; reason?: string }) =>
      apiClient.patch(`/api/v1/orders/${orderId}/cancel`, reason ? { reason } : {}),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ["admin-orders"] });
      qc.invalidateQueries({ queryKey: ["admin-order", vars.orderId] });
      qc.invalidateQueries({ queryKey: ["admin-order-summary"] });
      qc.invalidateQueries({ queryKey: ["my-orders"] });
    },
  });
}

export function useDeliveryCode(orderId: number, enabled: boolean) {
  return useQuery({
    queryKey: ["delivery-code", orderId],
    queryFn: () =>
      apiClient
        .get<{ code: string; expiresAt: string }>(`/api/v1/orders/${orderId}/delivery-code`)
        .then((r) => r.data),
    enabled,
    staleTime: 60_000,
    retry: false,
  });
}

export function useRefundOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId, reason }: { orderId: number; reason?: string }) =>
      apiClient.post(`/api/v1/admin/orders/${orderId}/refund`, reason ? { reason } : {}),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ["admin-orders"] });
      qc.invalidateQueries({ queryKey: ["admin-order", vars.orderId] });
      qc.invalidateQueries({ queryKey: ["admin-order-summary"] });
      qc.invalidateQueries({ queryKey: ["my-orders"] });
    },
  });
}
