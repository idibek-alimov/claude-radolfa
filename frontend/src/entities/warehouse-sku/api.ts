import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import type { SkuLookupResponse } from "./types";

export function useLookupSkuByBarcode() {
  return useMutation({
    mutationFn: (code: string) =>
      apiClient
        .get<SkuLookupResponse>("/api/v1/admin/warehouse/skus/by-barcode", { params: { code } })
        .then((r) => r.data),
  });
}

export function useSearchSkus(query: string, page: number) {
  return useQuery({
    queryKey: ["warehouse-sku-search", query, page],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<SkuLookupResponse>>("/api/v1/admin/warehouse/skus/search", {
          params: { query, page, size: 20 },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
    enabled: query.trim().length >= 2,
  });
}
