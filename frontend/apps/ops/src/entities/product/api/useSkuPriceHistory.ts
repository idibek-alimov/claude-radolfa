"use client";

import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { apiClient } from "@radolfa/shared/api";
import type { PaginatedResponse } from "@radolfa/shared/api/types";
import type { SkuPriceChange } from "@/entities/product/model/types";

export function useSkuPriceHistory(skuId: number | null, page: number) {
  return useQuery({
    queryKey: ["sku-price-history", skuId, page],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<SkuPriceChange>>(
          `/api/v1/admin/skus/${skuId}/price-history`,
          { params: { page, size: 20 } },
        )
        .then((r) => r.data),
    placeholderData: keepPreviousData,
    enabled: !!skuId,
  });
}
