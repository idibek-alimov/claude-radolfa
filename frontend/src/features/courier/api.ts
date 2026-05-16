import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { CourierOrder } from "@/entities/user";

export const DELIVERY_ATTEMPT_REASONS = [
  "NO_ANSWER",
  "WRONG_ADDRESS",
  "CUSTOMER_REFUSED",
  "PACKAGE_DAMAGED",
  "OTHER",
] as const;

export type DeliveryAttemptReason = (typeof DELIVERY_ATTEMPT_REASONS)[number];

export function useCourierOrders() {
  return useQuery({
    queryKey: ["courier-orders"],
    queryFn: () =>
      apiClient
        .get<CourierOrder[]>("/api/v1/courier/orders")
        .then((r) => r.data),
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
