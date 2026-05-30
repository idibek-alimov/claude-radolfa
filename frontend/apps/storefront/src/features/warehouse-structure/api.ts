import { useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { WarehouseZoneDto, WarehouseShelfDto, WarehouseBinDto } from "@/entities/warehouse-location";

export function useCreateZone() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { code: string; label?: string }) =>
      apiClient
        .post<WarehouseZoneDto>("/api/v1/admin/warehouse/zones", body)
        .then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["warehouse-zones"] });
    },
  });
}

export function useDeleteZone() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (zoneId: number) =>
      apiClient.delete(`/api/v1/admin/warehouse/zones/${zoneId}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["warehouse-zones"] });
    },
  });
}

export function useCreateShelf(zoneId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { code: string; label?: string }) =>
      apiClient
        .post<WarehouseShelfDto>(`/api/v1/admin/warehouse/zones/${zoneId}/shelves`, body)
        .then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["warehouse-shelves", zoneId] });
    },
  });
}

export function useDeleteShelf(zoneId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (shelfId: number) =>
      apiClient.delete(`/api/v1/admin/warehouse/shelves/${shelfId}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["warehouse-shelves", zoneId] });
    },
  });
}

export function useCreateBin(shelfId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { code: string }) =>
      apiClient
        .post<WarehouseBinDto>(`/api/v1/admin/warehouse/shelves/${shelfId}/bins`, body)
        .then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["warehouse-bins", shelfId] });
    },
  });
}

export function useDeleteBin(shelfId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (binId: number) =>
      apiClient.delete(`/api/v1/admin/warehouse/bins/${binId}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["warehouse-bins", shelfId] });
    },
  });
}
