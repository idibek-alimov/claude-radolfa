import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { CourierFleetEntry, CourierSummary } from "@/entities/user";

export function useFleetSummary() {
  return useQuery({
    queryKey: ["fleet-summary"],
    queryFn: () =>
      apiClient
        .get<CourierFleetEntry[]>("/api/v1/admin/fleet")
        .then((r) => r.data),
  });
}

export function useBulkReassign() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      orderIds,
      newCourierId,
    }: {
      orderIds: number[];
      newCourierId: number;
    }) =>
      apiClient.post("/api/v1/admin/fleet/reassign", { orderIds, newCourierId }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["fleet-summary"] }),
  });
}

export function useRedirectToPickpoint() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      orderId,
      pickpointId,
    }: {
      orderId: number;
      pickpointId: number;
    }) =>
      apiClient.post(`/api/v1/admin/orders/${orderId}/redirect-to-pickpoint`, {
        pickpointId,
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin-orders"] }),
  });
}

export function useRegenerateDeliveryCode() {
  return useMutation({
    mutationFn: (orderId: number) =>
      apiClient.post(`/api/v1/admin/orders/${orderId}/regenerate-delivery-code`),
  });
}

export function useCourierList() {
  return useQuery({
    queryKey: ["courier-list"],
    queryFn: () =>
      apiClient
        .get<CourierSummary[]>("/api/v1/admin/users/couriers")
        .then((r) => r.data),
  });
}
