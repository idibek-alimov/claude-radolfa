import { apiClient } from "@radolfa/shared/api";
import type { PaginatedResponse } from "@radolfa/shared/api/types";
import type { FeaturedCategory, FeaturedCategoryRequest } from "../model/types";

export interface FetchFeaturedCategoriesParams {
  page?: number; // 1-based
  size?: number;
  search?: string;
  sort?: string; // whitelisted: "displayOrder,asc|desc" | "createdAt,asc|desc"
}

/**
 * GET /api/v1/admin/featured-categories — MANAGER/ADMIN.
 * Server-side paginated list; search matches title/subtitle, sort is whitelisted.
 * Defaults mirror the backend: page=1, size=20, sort="displayOrder,asc".
 */
export async function fetchFeaturedCategories(
  params: FetchFeaturedCategoriesParams = {}
): Promise<PaginatedResponse<FeaturedCategory>> {
  const { page = 1, size = 20, search = "", sort = "displayOrder,asc" } = params;
  const { data } = await apiClient.get<PaginatedResponse<FeaturedCategory>>(
    "/api/v1/admin/featured-categories",
    { params: { page, size, search, sort } }
  );
  return data;
}

/** POST /api/v1/admin/featured-categories — MANAGER/ADMIN, returns 201 + created entry. */
export const createFeaturedCategory = (
  body: FeaturedCategoryRequest
): Promise<FeaturedCategory> =>
  apiClient.post("/api/v1/admin/featured-categories", body).then((r) => r.data);

/** PUT /api/v1/admin/featured-categories/{id} — MANAGER/ADMIN. */
export const updateFeaturedCategory = (
  id: number,
  body: FeaturedCategoryRequest
): Promise<FeaturedCategory> =>
  apiClient.put(`/api/v1/admin/featured-categories/${id}`, body).then((r) => r.data);

/** DELETE /api/v1/admin/featured-categories/{id} — MANAGER/ADMIN. */
export const deleteFeaturedCategory = (id: number): Promise<void> =>
  apiClient.delete(`/api/v1/admin/featured-categories/${id}`).then(() => undefined);

/**
 * POST /api/v1/admin/images/upload — MANAGER/ADMIN, multipart field "image".
 * Generic upload pipeline (Thumbnailator → S3); returns the resulting URL
 * to embed in the entry's `imageUrl`. Same primitive product-creation uses
 * (`uploadProductImage`) — duplicated here per FSD (features cannot
 * cross-import), not a new backend path.
 */
export async function uploadFeaturedCategoryImage(file: File): Promise<{ url: string }> {
  const form = new FormData();
  form.append("image", file);
  const { data } = await apiClient.post<{ url: string }>(
    "/api/v1/admin/images/upload",
    form,
    { headers: { "Content-Type": "multipart/form-data" } }
  );
  return data;
}
