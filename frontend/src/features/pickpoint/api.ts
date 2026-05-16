import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { PickpointOrder } from "@/entities/user";

export function usePickpointOrders() {
  return useQuery({
    queryKey: ["pickpoint-orders"],
    queryFn: () =>
      apiClient
        .get<PickpointOrder[]>("/api/v1/pickpoint/orders")
        .then((r) => r.data),
  });
}

export function useConfirmPickup() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId, code }: { orderId: number; code: string }) =>
      apiClient.post(`/api/v1/pickpoint/orders/${orderId}/confirm`, { code }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pickpoint-orders"] }),
  });
}
