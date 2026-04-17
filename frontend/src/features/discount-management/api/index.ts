import apiClient from "@/shared/api/axios";
import type {
  DiscountResponse,
  DiscountFormValues,
  DiscountListFilters,
  DiscountType,
  DiscountTypeFormValues,
  DiscountedProductRow,
  DiscountedProductFilters,
  CampaignSkuFilters,
} from "../model/types";
import type { PaginatedResponse } from "@/shared/api/types";

// ── Discount Types ────────────────────────────────────────────────

/** GET /api/v1/admin/discount-types — MANAGER+ */
export async function fetchDiscountTypes(): Promise<DiscountType[]> {
  const response = await apiClient.get<DiscountType[]>("/api/v1/admin/discount-types");
  return response.data;
}

/** POST /api/v1/admin/discount-types — MANAGER+ */
export async function createDiscountType(
  body: DiscountTypeFormValues
): Promise<DiscountType> {
  const response = await apiClient.post<DiscountType>("/api/v1/admin/discount-types", body);
  return response.data;
}

/** PUT /api/v1/admin/discount-types/{id} — MANAGER+ */
export async function updateDiscountType(
  id: number,
  body: DiscountTypeFormValues
): Promise<DiscountType> {
  const response = await apiClient.put<DiscountType>(
    `/api/v1/admin/discount-types/${id}`,
    body
  );
  return response.data;
}

/**
 * DELETE /api/v1/admin/discount-types/{id} — MANAGER+
 * Returns 409 if discounts are still using this type:
 *   { error: string, discountCount: number }
 */
export async function deleteDiscountType(id: number): Promise<void> {
  await apiClient.delete(`/api/v1/admin/discount-types/${id}`);
}

// ── Discounts ─────────────────────────────────────────────────────

/** GET /api/v1/admin/discounts — MANAGER+, paginated with optional filters */
export async function fetchDiscounts(
  filters: DiscountListFilters
): Promise<PaginatedResponse<DiscountResponse>> {
  const { typeId, status, from, to, search, sort, page, size } = filters;
  const response = await apiClient.get<PaginatedResponse<DiscountResponse>>(
    "/api/v1/admin/discounts",
    {
      params: {
        ...(typeId !== undefined && { typeId }),
        ...(status && status !== "all" && { status: status.toUpperCase() }),
        ...(from && { from }),
        ...(to && { to }),
        ...(search && { search }),
        ...(sort && { sort }),
        page,
        size,
      },
    }
  );
  return response.data;
}

/** GET /api/v1/admin/discounts/{id} — MANAGER+ */
export async function fetchDiscountById(id: number): Promise<DiscountResponse> {
  const response = await apiClient.get<DiscountResponse>(`/api/v1/admin/discounts/${id}`);
  return response.data;
}

/** POST /api/v1/admin/discounts — MANAGER+ */
export async function createDiscount(
  body: DiscountFormValues
): Promise<DiscountResponse> {
  const response = await apiClient.post<DiscountResponse>("/api/v1/admin/discounts", body);
  return response.data;
}

/** PUT /api/v1/admin/discounts/{id} — MANAGER+ */
export async function updateDiscount(
  id: number,
  body: DiscountFormValues
): Promise<DiscountResponse> {
  const response = await apiClient.put<DiscountResponse>(
    `/api/v1/admin/discounts/${id}`,
    body
  );
  return response.data;
}

/** PATCH /api/v1/admin/discounts/{id}/disable — MANAGER+ */
export async function disableDiscount(id: number): Promise<void> {
  await apiClient.patch(`/api/v1/admin/discounts/${id}/disable`);
}

/** PATCH /api/v1/admin/discounts/{id}/enable — MANAGER+ */
export async function enableDiscount(id: number): Promise<void> {
  await apiClient.patch(`/api/v1/admin/discounts/${id}/enable`);
}

// ── Bulk operations ───────────────────────────────────────────────

/** PATCH /api/v1/admin/discounts/bulk/enable — MANAGER+ */
export async function bulkEnableDiscounts(
  ids: number[]
): Promise<{ affected: number }> {
  const response = await apiClient.patch<{ affected: number }>(
    "/api/v1/admin/discounts/bulk/enable",
    { ids }
  );
  return response.data;
}

/** PATCH /api/v1/admin/discounts/bulk/disable — MANAGER+ */
export async function bulkDisableDiscounts(
  ids: number[]
): Promise<{ affected: number }> {
  const response = await apiClient.patch<{ affected: number }>(
    "/api/v1/admin/discounts/bulk/disable",
    { ids }
  );
  return response.data;
}

/** DELETE /api/v1/admin/discounts/bulk — MANAGER+ (body: { ids }) */
export async function bulkDeleteDiscounts(
  ids: number[]
): Promise<{ affected: number }> {
  const response = await apiClient.delete<{ affected: number }>(
    "/api/v1/admin/discounts/bulk",
    { data: { ids } }
  );
  return response.data;
}

/** POST /api/v1/admin/discounts/bulk/duplicate — MANAGER+ */
export async function bulkDuplicateDiscounts(
  ids: number[]
): Promise<DiscountResponse[]> {
  const response = await apiClient.post<DiscountResponse[]>(
    "/api/v1/admin/discounts/bulk/duplicate",
    { ids }
  );
  return response.data;
}

// ── Discounted products ───────────────────────────────────────────

/** GET /api/v1/admin/discounts/products — paginated, server-side search/filter/sort */
export async function fetchDiscountedProducts(
  filters: DiscountedProductFilters
): Promise<PaginatedResponse<DiscountedProductRow>> {
  const { search, campaignId, minDeltaPercent, maxDeltaPercent, sort, page, size } = filters;
  const response = await apiClient.get<PaginatedResponse<DiscountedProductRow>>(
    "/api/v1/admin/discounts/products",
    {
      params: {
        ...(search && { search }),
        ...(campaignId !== undefined && { campaignId }),
        ...(minDeltaPercent !== undefined && { minDeltaPercent }),
        ...(maxDeltaPercent !== undefined && { maxDeltaPercent }),
        ...(sort && { sort }),
        page,
        size,
      },
    }
  );
  return response.data;
}

/** GET /api/v1/admin/discounts/{id}/skus — paginated SKUs for one campaign, with search */
export async function fetchCampaignSkus(
  campaignId: number,
  filters: CampaignSkuFilters
): Promise<PaginatedResponse<DiscountedProductRow>> {
  const { search, page, size } = filters;
  const response = await apiClient.get<PaginatedResponse<DiscountedProductRow>>(
    `/api/v1/admin/discounts/${campaignId}/skus`,
    {
      params: {
        ...(search && { search }),
        page,
        size,
      },
    }
  );
  return response.data;
}
