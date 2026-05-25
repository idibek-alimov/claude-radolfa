import { useQuery, useMutation, useQueryClient, keepPreviousData } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { CourierOrder } from "@/entities/user";
import type { PaginatedResponse } from "@/shared/api/types";
import type { OrderStatus } from "@/entities/order/model/types";

export const DELIVERY_ATTEMPT_REASONS = [
  "NO_ANSWER",
  "WRONG_ADDRESS",
  "CUSTOMER_REFUSED",
  "PACKAGE_DAMAGED",
  "OTHER",
] as const;

export type DeliveryAttemptReason = (typeof DELIVERY_ATTEMPT_REASONS)[number];

export function useCourierOrders(statuses: OrderStatus[], page: number, size: number = 20) {
  return useQuery({
    queryKey: ["courier-orders", statuses.join(","), page, size],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<CourierOrder>>("/api/v1/courier/orders", {
          params: { statuses: statuses.join(","), page, size },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
    enabled: statuses.length > 0,
  });
}

export function useMarkCollected() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (orderId: number) =>
      apiClient.post(`/api/v1/courier/orders/${orderId}/collect`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["courier-orders"] }),
  });
}

export function useConfirmDelivery() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId, code }: { orderId: number; code: string }) =>
      apiClient.post(`/api/v1/courier/orders/${orderId}/confirm`, { code }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["courier-orders"] }),
  });
}

export function useMarkAttempted() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      orderId,
      reason,
      photoUrl,
    }: {
      orderId: number;
      reason: DeliveryAttemptReason;
      photoUrl?: string;
    }) =>
      apiClient.post(`/api/v1/courier/orders/${orderId}/attempt`, {
        reason,
        photoUrl,
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["courier-orders"] }),
  });
}
