import { apiClient } from "@/shared/api";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useTranslations } from "next-intl";
import { getErrorMessage } from "@/shared/lib";
import type { PaginatedResponse } from "@/shared/api/types";
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
