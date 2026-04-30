import { apiClient } from "@/shared/api";
import type { CategoryTree } from "@/entities/product/model/types";

export async function createCategory(
  name: string,
  parentId: number | null,
  traitIds?: number[]
): Promise<CategoryTree> {
  const { data } = await apiClient.post<CategoryTree>("/api/v1/admin/categories", {
    name,
    parentId,
    traitIds: traitIds ?? [],
  });
  return data;
}

export async function updateCategory(
  id: number,
  body: { name: string; parentId: number | null; traitIds?: number[] }
): Promise<void> {
  await apiClient.patch(`/api/v1/admin/categories/${id}`, body);
}

export async function deleteCategory(categoryId: number): Promise<void> {
  await apiClient.delete(`/api/v1/admin/categories/${categoryId}`);
}

export async function fetchCategoryTraitIds(id: number): Promise<number[]> {
  const { data } = await apiClient.get<number[]>(`/api/v1/admin/categories/${id}/traits`);
  return data;
}
