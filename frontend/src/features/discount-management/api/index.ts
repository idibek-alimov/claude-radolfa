import apiClient from "@/shared/api/axios";
import type {
  DiscountResponse,
  DiscountFormValues,
  DiscountListFilters,
  DiscountType,
  DiscountTypeFormValues,
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
  const { typeId, status, from, to, page, size } = filters;
  const response = await apiClient.get<PaginatedResponse<DiscountResponse>>(
    "/api/v1/admin/discounts",
    {
      params: {
        ...(typeId !== undefined && { typeId }),
        ...(status && status !== "all" && { status: status.toUpperCase() }),
        ...(from && { from }),
        ...(to && { to }),
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
