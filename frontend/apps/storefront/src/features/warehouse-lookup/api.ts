import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import type { InventoryTransactionRecord } from "./types";

export { useLookupSkuByBarcode } from "@/entities/warehouse-sku";
export { useWarehouseZones, useShelvesByZone, useBinsByShelf } from "@/entities/warehouse-location";

export function useRelocateStock() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      skuId,
      fromBinId,
      toBinId,
      quantity,
    }: {
      skuId: number;
      fromBinId: number;
      toBinId: number;
      quantity: number;
    }) =>
      apiClient.post(`/api/v1/admin/warehouse/skus/${skuId}/relocate`, {
        fromBinId,
        toBinId,
        quantity,
      }),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ["warehouse-inventory-history", variables.skuId] });
      qc.invalidateQueries({ queryKey: ["inbound-queue"] });
    },
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
