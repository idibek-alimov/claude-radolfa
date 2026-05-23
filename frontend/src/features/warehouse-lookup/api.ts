import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import type { InventoryTransactionRecord } from "./types";

export { useLookupSkuByBarcode } from "@/entities/warehouse-sku";
export { useWarehouseZones, useShelvesByZone, useBinsByShelf } from "@/entities/warehouse-location";

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
