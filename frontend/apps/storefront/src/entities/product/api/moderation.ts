import { apiClient } from "@radolfa/shared/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useTranslations } from "next-intl";
import { getErrorMessage } from "@radolfa/shared/lib";
import type { PaginatedResponse } from "@radolfa/shared/api/types";
import type { AdminProductRow } from "@/entities/product/model/types";
import type { ProductStatus } from "@/entities/product/model/types";

export interface AdminProductsParams {
  status?: ProductStatus;
  search?: string;
  page: number;
  size: number;
}

export async function fetchAdminProducts(
  params: AdminProductsParams
): Promise<PaginatedResponse<AdminProductRow>> {
  const { data } = await apiClient.get<PaginatedResponse<AdminProductRow>>(
    "/api/v1/admin/products",
    { params: { status: params.status, search: params.search, page: params.page, size: params.size } }
  );
  return data;
}

export async function fetchPendingProductCount(): Promise<{ count: number }> {
  const { data } = await apiClient.get<{ count: number }>(
    "/api/v1/admin/products/pending-count"
  );
  return data;
}

export async function submitProductForReview(productBaseId: number): Promise<void> {
  await apiClient.post(`/api/v1/admin/products/${productBaseId}/submit-for-review`);
}

export async function approveProduct(productBaseId: number): Promise<void> {
  await apiClient.post(`/api/v1/admin/products/${productBaseId}/approve`);
}

export async function rejectProduct(
  productBaseId: number,
  rejectionReason: string
): Promise<void> {
  await apiClient.post(`/api/v1/admin/products/${productBaseId}/reject`, { rejectionReason });
}

export function useSubmitProductForReview() {
  const qc = useQueryClient();
  const t = useTranslations("manage.products.toast");

  return useMutation({
    mutationFn: submitProductForReview,
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ["admin-product", id] });
      qc.invalidateQueries({ queryKey: ["admin-products"] });
      qc.invalidateQueries({ queryKey: ["admin-products-pending-count"] });
      toast.success(t("submitted"));
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useApproveProduct() {
  const qc = useQueryClient();
  const t = useTranslations("manage.products.toast");

  return useMutation({
    mutationFn: approveProduct,
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ["admin-products"] });
      qc.invalidateQueries({ queryKey: ["admin-product", id] });
      qc.invalidateQueries({ queryKey: ["admin-products-pending-count"] });
      toast.success(t("approved"));
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useRejectProduct() {
  const qc = useQueryClient();
  const t = useTranslations("manage.products.toast");

  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) =>
      rejectProduct(id, reason),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: ["admin-products"] });
      qc.invalidateQueries({ queryKey: ["admin-product", id] });
      qc.invalidateQueries({ queryKey: ["admin-products-pending-count"] });
      toast.success(t("rejected"));
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function usePendingProductCount(enabled = true) {
  return useQuery({
    queryKey: ["admin-products-pending-count"],
    queryFn: fetchPendingProductCount,
    enabled,
    refetchInterval: 60_000,
    staleTime: 30_000,
  });
}
