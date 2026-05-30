import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { PaginatedResponse } from "@radolfa/shared/api/types";
import type { InboundQueueItem } from "./types";

export function useInboundQueue(page: number, search: string) {
  return useQuery({
    queryKey: ["inbound-queue", page, search],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<InboundQueueItem>>("/api/v1/admin/warehouse/inbound-queue", {
          params: { page, size: 20, search },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
  });
}

export function usePutaway() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ skuId, binId, quantity }: { skuId: number; binId: number; quantity: number }) =>
      apiClient.post(`/api/v1/admin/warehouse/skus/${skuId}/putaway`, { binId, quantity }),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ["inbound-queue"] });
      qc.invalidateQueries({ queryKey: ["warehouse-inventory-history", variables.skuId] });
    },
  });
}
