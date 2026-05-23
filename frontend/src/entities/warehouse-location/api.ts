import { useQuery } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { WarehouseZoneDto, WarehouseShelfDto, WarehouseBinDto } from "./types";

export function useWarehouseZones() {
  return useQuery({
    queryKey: ["warehouse-zones"],
    queryFn: () =>
      apiClient.get<WarehouseZoneDto[]>("/api/v1/admin/warehouse/zones").then((r) => r.data),
  });
}

export function useShelvesByZone(zoneId: number | null) {
  return useQuery({
    queryKey: ["warehouse-shelves", zoneId],
    queryFn: () =>
      apiClient
        .get<WarehouseShelfDto[]>(`/api/v1/admin/warehouse/zones/${zoneId}/shelves`)
        .then((r) => r.data),
    enabled: zoneId != null,
  });
}

export function useBinsByShelf(shelfId: number | null) {
  return useQuery({
    queryKey: ["warehouse-bins", shelfId],
    queryFn: () =>
      apiClient
        .get<WarehouseBinDto[]>(`/api/v1/admin/warehouse/shelves/${shelfId}/bins`)
        .then((r) => r.data),
    enabled: shelfId != null,
  });
}
