import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { PaginatedResponse } from "@radolfa/shared/api/types";
import type { PickQueueItem, PickSession, ScanResult } from "./types";

export function usePickQueue(page: number, search: string) {
  return useQuery({
    queryKey: ["warehouse-pick-queue", page, search],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<PickQueueItem>>("/api/v1/admin/warehouse/pick-queue", {
          params: { page, size: 20, search },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
  });
}

export function usePickSession(orderId: number) {
  return useQuery({
    queryKey: ["warehouse-pick-session", orderId],
    queryFn: () =>
      apiClient
        .get<PickSession>(`/api/v1/admin/warehouse/pick-sessions/${orderId}`)
        .then((r) => r.data),
  });
}

export function useScanUnit(orderId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (scannedBarcode: string) =>
      apiClient
        .post<ScanResult>(`/api/v1/admin/warehouse/pick-sessions/${orderId}/scan`, {
          scannedBarcode,
        })
        .then((r) => r.data),
    onSuccess: (result) => {
      qc.invalidateQueries({ queryKey: ["warehouse-pick-session", orderId] });
      if (result.orderFullyPicked) {
        qc.invalidateQueries({ queryKey: ["warehouse-pick-queue"] });
        qc.invalidateQueries({ queryKey: ["admin-order", orderId] });
        qc.invalidateQueries({ queryKey: ["admin-orders"] });
        qc.invalidateQueries({ queryKey: ["admin-order-summary"] });
      }
    },
  });
}

export function useCompletePickSession(orderId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () =>
      apiClient.post(`/api/v1/admin/warehouse/pick-sessions/${orderId}/complete`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["warehouse-pick-session", orderId] });
      qc.invalidateQueries({ queryKey: ["warehouse-pick-queue"] });
      qc.invalidateQueries({ queryKey: ["admin-order", orderId] });
      qc.invalidateQueries({ queryKey: ["admin-orders"] });
      qc.invalidateQueries({ queryKey: ["admin-order-summary"] });
    },
  });
}
