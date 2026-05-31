import { apiClient } from "@radolfa/shared/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { getErrorMessage } from "@radolfa/shared/lib";
import type { PaginatedResponse } from "@radolfa/shared/api/types";
import type { Seller, SellerOrderItem } from "@/entities/seller/model/types";
import type { AdminProductRow, ProductCard } from "@/entities/product/model/types";
import type { ProductStatus } from "@/entities/product/model/types";
import { buildPayload } from "@/features/product-creation/api/createProduct";
import type { WizardState } from "@/features/product-creation/model/types";

// ── Query params ───────────────────────────────────────────────────────────

export interface SellerProductsParams {
  status?: ProductStatus;
  search?: string;
  page: number;
  size: number;
}

export interface SellerOrdersParams {
  search?: string;
  sortBy?: string;
  sortDir?: string;
  page: number;
  size: number;
}

export interface UpdateStockPayload {
  quantity?: number;
  delta?: number;
}

// ── API functions ──────────────────────────────────────────────────────────

export async function fetchMyProfile(): Promise<Seller> {
  const { data } = await apiClient.get<Seller>("/api/v1/seller/me");
  return data;
}

export async function fetchMyProducts(
  params: SellerProductsParams
): Promise<PaginatedResponse<AdminProductRow>> {
  const { data } = await apiClient.get<PaginatedResponse<AdminProductRow>>(
    "/api/v1/seller/me/products",
    { params: { status: params.status, search: params.search, page: params.page, size: params.size } }
  );
  return data;
}

export async function fetchMyProductCard(productBaseId: number): Promise<ProductCard> {
  const { data } = await apiClient.get<ProductCard>(
    `/api/v1/seller/me/products/${productBaseId}`
  );
  return data;
}

export async function createMyProduct(state: WizardState): Promise<{ productBaseId: number }> {
  const { data } = await apiClient.post<{ productBaseId: number }>(
    "/api/v1/seller/me/products",
    buildPayload(state)
  );
  return data;
}

export async function submitMyProductForReview(productBaseId: number): Promise<void> {
  await apiClient.post(
    `/api/v1/seller/me/products/${productBaseId}/submit-for-review`
  );
}

export async function updateMySkuPrice(
  skuId: number,
  price: number
): Promise<void> {
  await apiClient.put(`/api/v1/seller/me/skus/${skuId}/price`, { price });
}

export async function updateMySkuStock(
  skuId: number,
  payload: UpdateStockPayload
): Promise<void> {
  await apiClient.put(`/api/v1/seller/me/skus/${skuId}/stock`, payload);
}

export async function fetchMyOrders(
  params: SellerOrdersParams
): Promise<PaginatedResponse<SellerOrderItem>> {
  const { data } = await apiClient.get<PaginatedResponse<SellerOrderItem>>(
    "/api/v1/seller/me/orders",
    {
      params: {
        search: params.search,
        sortBy: params.sortBy ?? "orderCreatedAt",
        sortDir: params.sortDir ?? "DESC",
        page: params.page,
        size: params.size,
      },
    }
  );
  return data;
}

// ── TanStack Query hooks ───────────────────────────────────────────────────

export function useMyProfile() {
  return useQuery({
    queryKey: ["seller-profile"],
    queryFn: fetchMyProfile,
    staleTime: 60_000,
  });
}

export function useMyProducts(params: SellerProductsParams) {
  return useQuery({
    queryKey: ["seller-products", params],
    queryFn: () => fetchMyProducts(params),
  });
}

export function useMyProductCard(productBaseId: number) {
  return useQuery({
    queryKey: ["seller-product", productBaseId],
    queryFn: () => fetchMyProductCard(productBaseId),
    enabled: Number.isFinite(productBaseId) && productBaseId > 0,
  });
}

export function useMyOrders(params: SellerOrdersParams) {
  return useQuery({
    queryKey: ["seller-orders", params],
    queryFn: () => fetchMyOrders(params),
  });
}

export function useSubmitMyProductForReview() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: submitMyProductForReview,
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ["seller-products"] });
      qc.invalidateQueries({ queryKey: ["seller-product", id] });
      toast.success("Submitted for review");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useUpdateMySkuPrice() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ skuId, price }: { skuId: number; price: number }) =>
      updateMySkuPrice(skuId, price),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["seller-products"] });
      qc.invalidateQueries({ queryKey: ["seller-product"] });
      toast.success("Price updated");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useUpdateMySkuStock() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ skuId, payload }: { skuId: number; payload: UpdateStockPayload }) =>
      updateMySkuStock(skuId, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["seller-products"] });
      qc.invalidateQueries({ queryKey: ["seller-product"] });
      toast.success("Stock updated");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}
