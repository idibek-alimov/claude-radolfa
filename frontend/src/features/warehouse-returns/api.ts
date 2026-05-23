import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import apiClient from "@/shared/api/axios";
import { getErrorMessage } from "@/shared/lib";
import type { PaginatedResponse } from "@/shared/api/types";
import type { CustomerReturn, Resellability } from "@/entities/pickpoint";

export interface ItemReview {
  orderItemId: number;
  resellability: Resellability;
}

export function useWarehouseReturnsQueue(page: number) {
  return useQuery({
    queryKey: ["warehouse-returns-queue", page],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<CustomerReturn>>("/api/v1/admin/warehouse/customer-returns", {
          params: { page, size: 20 },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
  });
}

export function useWarehouseReturn(id: number) {
  return useQuery({
    queryKey: ["warehouse-return", id],
    queryFn: () =>
      apiClient
        .get<CustomerReturn>(`/api/v1/admin/warehouse/customer-returns/${id}`)
        .then((r) => r.data),
    enabled: id > 0,
  });
}

export function useReviewReturnItems(returnId: number) {
  const qc = useQueryClient();
  const t = useTranslations("warehouse");
  return useMutation({
    mutationFn: (reviews: ItemReview[]) =>
      apiClient.post(`/api/v1/admin/warehouse/customer-returns/${returnId}/review-items`, { reviews }),
    onSuccess: () => {
      toast.success(t("returns.review.successToast"));
      qc.invalidateQueries({ queryKey: ["warehouse-returns-queue"] });
      qc.invalidateQueries({ queryKey: ["warehouse-return", returnId] });
    },
    onError: (err) => toast.error(getErrorMessage(err, t("returns.review.errorToast"))),
  });
}
