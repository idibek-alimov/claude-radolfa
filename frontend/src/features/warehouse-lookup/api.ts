import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import type {
  InventoryTransactionRecord,
  WarehouseZoneDto,
  WarehouseShelfDto,
  WarehouseBinDto,
} from "./types";

export { useLookupSkuByBarcode } from "@/entities/warehouse-sku";

export function useAssignSkuToBin() {
  return useMutation({
    mutationFn: ({ skuId, binId }: { skuId: number; binId: number | null }) =>
      apiClient.put(`/api/v1/admin/warehouse/skus/${skuId}/bin`, { binId }),
  });
}

export function useInventoryHistory(skuId: number | null, page: number) {
  return useQuery({
    queryKey: ["warehouse-inventory-history", skuId, page],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<InventoryTransactionRecord>>(
          `/api/v1/admin/warehouse/skus/${skuId}/inventory-history`,
          { params: { page, size: 20 } },
        )
        .then((r) => r.data),
    placeholderData: keepPreviousData,
    enabled: !!skuId,
  });
}

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
