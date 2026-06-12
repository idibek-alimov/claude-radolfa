import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { PaginatedResponse } from "@radolfa/shared/api/types";
import type { CreateStockReceiptRequest, StockReceiptDto } from "./types";

export function useStockReceipts(page: number, search: string) {
  return useQuery({
    queryKey: ["stock-receipts", page, search],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<StockReceiptDto>>("/api/v1/admin/warehouse/stock-receipts", {
          params: { page, size: 20, search },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
  });
}

export function useStockReceipt(id: number) {
  return useQuery({
    queryKey: ["stock-receipt", id],
    queryFn: () =>
      apiClient
        .get<StockReceiptDto>(`/api/v1/admin/warehouse/stock-receipts/${id}`)
        .then((r) => r.data),
    enabled: id > 0,
  });
}

export function useCreateStockReceipt() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateStockReceiptRequest) =>
      apiClient
        .post<StockReceiptDto>("/api/v1/admin/warehouse/stock-receipts", body)
        .then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["stock-receipts"] });
    },
  });
}
